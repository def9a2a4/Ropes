package anon.def9a2a4.ropes;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class Items {
    private final RopesPlugin plugin;
    private final NamespacedKey ROPE_LENGTH_KEY;
    private final NamespacedKey ARROW_ROPE_LENGTH_KEY;
    private final MiniMessage miniMessage;

    public Items(RopesPlugin plugin) {
        this.plugin = plugin;
        this.ROPE_LENGTH_KEY = new NamespacedKey(plugin, "rope_length");
        this.ARROW_ROPE_LENGTH_KEY = new NamespacedKey(plugin, "arrow_rope_length");
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Parses a MiniMessage string with placeholder replacements.
     * Automatically disables italic decoration (Minecraft default for custom items).
     */
    private Component parseText(String template, TagResolver... resolvers) {
        Component component = miniMessage.deserialize(template, TagResolver.resolver(resolvers));
        return component.decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Parses lore lines from templates with placeholders.
     */
    private List<Component> parseLore(List<String> templates, TagResolver... resolvers) {
        return templates.stream()
            .map(template -> parseText(template, resolvers))
            .toList();
    }

    public ItemStack createRopeCoil(int meters) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        // Set custom texture
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", plugin.getConfiguration().getHeadTexture()));
        meta.setPlayerProfile(profile);

        // Get item config
        Config.ItemDisplayConfig itemConfig = plugin.getConfiguration().getRopeCoilItemConfig();

        // Create placeholder resolver
        TagResolver metersPlaceholder = Placeholder.unparsed("meters", String.valueOf(meters));

        // Set display name with MiniMessage
        meta.displayName(parseText(itemConfig.nameTemplate(), metersPlaceholder));

        // Set lore with MiniMessage
        meta.lore(parseLore(itemConfig.loreTemplates(), metersPlaceholder));

        // Store length in PDC
        meta.getPersistentDataContainer().set(ROPE_LENGTH_KEY, PersistentDataType.INTEGER, meters);

        head.setItemMeta(meta);
        return head;
    }

    public int getRopeLength(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(ROPE_LENGTH_KEY, PersistentDataType.INTEGER, 0);
    }

    public boolean isRopeCoil(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ROPE_LENGTH_KEY, PersistentDataType.INTEGER);
    }

    public ItemStack combineCoils(ItemStack coil1, ItemStack coil2) {
        int length1 = getRopeLength(coil1);
        int length2 = getRopeLength(coil2);
        int combined = Math.min(length1 + length2, plugin.getConfiguration().getRopeCoilMaxLength());
        return createRopeCoil(combined);
    }

    public ItemStack createRopeArrow(int ropeLength) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();

        // Get item config
        Config.ItemDisplayConfig itemConfig = plugin.getConfiguration().getRopeArrowItemConfig();

        // Create placeholder resolver
        TagResolver metersPlaceholder = Placeholder.unparsed("meters", String.valueOf(ropeLength));

        // Set display name with MiniMessage
        meta.displayName(parseText(itemConfig.nameTemplate(), metersPlaceholder));

        // Set lore with MiniMessage
        meta.lore(parseLore(itemConfig.loreTemplates(), metersPlaceholder));

        // Add enchantment glint if configured
        if (plugin.getConfiguration().isRopeArrowGlint()) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Store rope length in PDC
        meta.getPersistentDataContainer().set(ARROW_ROPE_LENGTH_KEY, PersistentDataType.INTEGER, ropeLength);

        arrow.setItemMeta(meta);
        return arrow;
    }

    public boolean isRopeArrow(ItemStack item) {
        if (item == null || item.getType() != Material.ARROW) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ARROW_ROPE_LENGTH_KEY, PersistentDataType.INTEGER);
    }

    public int getArrowRopeLength(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(ARROW_ROPE_LENGTH_KEY, PersistentDataType.INTEGER, 0);
    }

    public NamespacedKey getRopeLengthKey() {
        return ROPE_LENGTH_KEY;
    }

    public NamespacedKey getArrowRopeLengthKey() {
        return ARROW_ROPE_LENGTH_KEY;
    }
}
