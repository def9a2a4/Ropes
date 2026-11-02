package anon.def9a2a4.ropes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Commands implements CommandExecutor, TabCompleter {
    private final RopesPlugin plugin;
    // Subcommand -> permission needed to use it (null = everyone); also drives tab completion
    private static final Map<String, String> SUBCOMMANDS = new LinkedHashMap<>();
    static {
        SUBCOMMANDS.put("reload", "ropes.admin");
        SUBCOMMANDS.put("info", "ropes.use");
        SUBCOMMANDS.put("delete_all", "ropes.admin");
        SUBCOMMANDS.put("give", "ropes.give");
        SUBCOMMANDS.put("recipes", "ropes.recipes");
        SUBCOMMANDS.put("help", null);
    }
    private static final List<String> GIVE_TYPES = List.of("coil", "arrow");
    private static final String GITHUB_URL = "https://github.com/def9a2a4/Ropes";
    private static final String DISCORD_URL = "https://def9a2a4.github.io/discord.html";

    public Commands(RopesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleHelp(sender);
        }

        String subcommand = args[0].toLowerCase();

        return switch (subcommand) {
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender);
            case "delete_all" -> handleDeleteAll(sender, args);
            case "give" -> handleGive(sender, args);
            case "recipes" -> handleRecipes(sender, args);
            case "help" -> handleHelp(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand. Use /ropes help for a list of commands.", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ropes.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        plugin.getConfiguration().load();
        sender.sendMessage(Component.text("Configuration reloaded.", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Note: Recipe changes require a server restart.", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("ropes.use")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        Map<String, Integer> ropeCounts = new HashMap<>();
        int totalRopes = 0;

        for (World world : Bukkit.getWorlds()) {
            int worldCount = 0;
            for (Entity entity : world.getEntities()) {
                if (plugin.getDisplay().isRopeDisplay(entity)) {
                    worldCount++;
                }
            }
            if (worldCount > 0) {
                ropeCounts.put(world.getName(), worldCount);
                totalRopes += worldCount;
            }
        }

        sender.sendMessage(Component.text("=== Ropes Info ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Total rope segments: ", NamedTextColor.YELLOW)
            .append(Component.text(totalRopes, NamedTextColor.WHITE)));

        if (ropeCounts.isEmpty()) {
            sender.sendMessage(Component.text("No ropes currently placed.", NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("By world:", NamedTextColor.YELLOW));
            for (Map.Entry<String, Integer> entry : ropeCounts.entrySet()) {
                sender.sendMessage(Component.text("  " + entry.getKey() + ": ", NamedTextColor.GRAY)
                    .append(Component.text(entry.getValue(), NamedTextColor.WHITE)));
            }
        }

        return true;
    }

    private boolean handleDeleteAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ropes.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            sender.sendMessage(Component.text("Warning: This will delete ALL ropes in loaded chunks, in every world!", NamedTextColor.RED));
            sender.sendMessage(Component.text("Ropes in unloaded chunks are not affected.", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("To confirm, run: /ropes delete_all confirm", NamedTextColor.YELLOW));
            return true;
        }

        int totalRemoved = 0;

        for (World world : Bukkit.getWorlds()) {
            List<Entity> toRemove = new ArrayList<>();
            for (Entity entity : world.getEntities()) {
                if (plugin.getDisplay().isRopeDisplay(entity)) {
                    // Remove the associated chain block
                    entity.getLocation().getBlock().setType(org.bukkit.Material.AIR);
                    toRemove.add(entity);
                }
            }
            for (Entity entity : toRemove) {
                entity.remove();
                totalRemoved++;
            }
        }

        sender.sendMessage(Component.text("Deleted " + totalRemoved + " rope segments.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ropes.give")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ropes give <coil|arrow> [length]", NamedTextColor.RED));
            return true;
        }

        String itemType = args[1].toLowerCase();
        int length = plugin.getConfiguration().getRopeCoilDefaultLength();

        if (args.length >= 3) {
            try {
                length = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid length. Must be a positive number.", NamedTextColor.RED));
                return true;
            }
        }

        if (length <= 0) {
            sender.sendMessage(Component.text("Length must be a positive number.", NamedTextColor.RED));
            return true;
        }

        ItemStack item;
        String itemName;

        switch (itemType) {
            case "coil" -> {
                item = plugin.getItems().createRopeCoil(length);
                itemName = "Rope Coil";
            }
            case "arrow" -> {
                item = plugin.getItems().createRopeArrow(length);
                itemName = "Rope Arrow";
            }
            default -> {
                sender.sendMessage(Component.text("Unknown item type. Use 'coil' or 'arrow'.", NamedTextColor.RED));
                return true;
            }
        }

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            sender.sendMessage(Component.text("Inventory full. " + itemName + " dropped at your feet.", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Gave " + itemName + " (" + length + "m).", NamedTextColor.GREEN));
        }

        return true;
    }

    private boolean handleRecipes(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ropes.recipes")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("Usage: /ropes recipes <player>", NamedTextColor.RED));
            return true;
        }

        unlockRopesRecipes(target);

        if (target.equals(sender)) {
            sender.sendMessage(Component.text("Unlocked all Ropes recipes.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Unlocked all Ropes recipes for " + target.getName() + ".", NamedTextColor.GREEN));
        }
        return true;
    }

    private void unlockRopesRecipes(Player player) {
        player.discoverRecipe(new NamespacedKey(plugin, "rope_coil"));
        player.discoverRecipe(new NamespacedKey(plugin, "rope_coil_combine"));
        player.discoverRecipe(new NamespacedKey(plugin, "rope_arrow"));
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Ropes v" + plugin.getPluginMeta().getVersion() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Place, shoot, and climb ropes.", NamedTextColor.GRAY));
        helpLine(sender, null, "/ropes help", "Show this help message");
        helpLine(sender, "ropes.use", "/ropes info", "Show info about all placed ropes");
        helpLine(sender, "ropes.give", "/ropes give <coil|arrow> [length]", "Give yourself a rope item");
        helpLine(sender, "ropes.recipes", "/ropes recipes [player]", "Unlock rope recipes");
        helpLine(sender, "ropes.admin", "/ropes reload", "Reload the configuration");
        helpLine(sender, "ropes.admin", "/ropes delete_all", "Delete all ropes in loaded chunks");
        sender.sendMessage(link("GitHub", GITHUB_URL));
        sender.sendMessage(link("Discord", DISCORD_URL));
        return true;
    }

    /** Shows a help line only to senders who can use the command (null permission = everyone). */
    private static void helpLine(CommandSender sender, String permission, String usage, String description) {
        if (permission != null && !sender.hasPermission(permission)) return;
        sender.sendMessage(Component.text(usage, NamedTextColor.YELLOW)
            .append(Component.text(" - " + description, NamedTextColor.GRAY)));
    }

    private static Component link(String label, String url) {
        return Component.text(label + ": ", NamedTextColor.GRAY)
            .append(Component.text(url, NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Map.Entry<String, String> sub : SUBCOMMANDS.entrySet()) {
                boolean allowed = sub.getValue() == null || sender.hasPermission(sub.getValue());
                if (allowed && sub.getKey().startsWith(partial)) {
                    completions.add(sub.getKey());
                }
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            String partial = args[1].toLowerCase();

            if (subcommand.equals("give")) {
                for (String type : GIVE_TYPES) {
                    if (type.startsWith(partial)) {
                        completions.add(type);
                    }
                }
            } else if (subcommand.equals("delete_all")) {
                if ("confirm".startsWith(partial)) {
                    completions.add("confirm");
                }
            } else if (subcommand.equals("recipes")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("give")) {
                // Suggest some common lengths
                completions.addAll(List.of("1", "2", "4", "8", "16"));
            }
        }

        return completions;
    }
}
