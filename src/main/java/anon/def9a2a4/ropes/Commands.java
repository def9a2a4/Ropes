package anon.def9a2a4.ropes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Map;

public class Commands implements CommandExecutor, TabCompleter {
    private final RopesPlugin plugin;
    private static final List<String> SUBCOMMANDS = List.of("reload", "info", "delete_all", "give", "help");
    private static final List<String> GIVE_TYPES = List.of("coil", "arrow");

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
            sender.sendMessage(Component.text("Warning: This will delete ALL placed ropes in ALL worlds!", NamedTextColor.RED));
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

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Ropes Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/ropes help", NamedTextColor.YELLOW)
            .append(Component.text(" - Show this help message", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ropes info", NamedTextColor.YELLOW)
            .append(Component.text(" - Show info about all placed ropes", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ropes give <coil|arrow> [length]", NamedTextColor.YELLOW)
            .append(Component.text(" - Give yourself a rope item", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ropes reload", NamedTextColor.YELLOW)
            .append(Component.text(" - Reload the configuration", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/ropes delete_all confirm", NamedTextColor.YELLOW)
            .append(Component.text(" - Delete all placed ropes", NamedTextColor.GRAY)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
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
