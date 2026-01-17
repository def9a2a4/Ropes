package anon.def9a2a4.ropes;

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

import java.net.URL;
import java.util.Base64;
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

        // Set custom texture using URL extraction (works for recipe book display)
        applyTextureFromBase64(meta, plugin.getConfiguration().getHeadTexture());

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

    /**
     * Applies a custom texture to a player head from a Base64-encoded texture value.
     * Extracts the skin URL from the base64 JSON and uses setOwnerProfile for proper
     * recipe book display.
     */
    private void applyTextureFromBase64(SkullMeta meta, String textureBase64) {
        if (textureBase64 == null || textureBase64.isEmpty()) {
            return;
        }

        try {
            // Decode Base64 to get texture URL (format: {"textures":{"SKIN":{"url":"..."}}})
            String decoded = new String(Base64.getDecoder().decode(textureBase64));

            // Extract URL from JSON
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd = decoded.indexOf("\"", urlStart);

            if (urlStart > 7 && urlEnd > urlStart) {
                String urlString = decoded.substring(urlStart, urlEnd);
                URL textureUrl = new URL(urlString);

                // Create player profile with texture via URL
                var profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                var textures = profile.getTextures();
                textures.setSkin(textureUrl);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply head texture: " + e.getMessage());
        }
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

    // ==================== Recipe Book Placeholder Items ====================
    // These methods create items specifically for recipe book display.
    // They have different names/lore than the actual crafted items.

    /**
     * Creates a rope coil placeholder for the recipe book.
     * Same as normal but with "(unshaped)" added to lore.
     */
    public ItemStack createRopeCoilForRecipe(int meters) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        applyTextureFromBase64(meta, plugin.getConfiguration().getHeadTexture());

        Config.ItemDisplayConfig itemConfig = plugin.getConfiguration().getRopeCoilItemConfig();
        TagResolver metersPlaceholder = Placeholder.unparsed("meters", String.valueOf(meters));

        meta.displayName(parseText(itemConfig.nameTemplate(), metersPlaceholder));

        // Add "(unshaped)" to the lore
        List<Component> lore = new java.util.ArrayList<>(parseLore(itemConfig.loreTemplates(), metersPlaceholder));
        lore.add(parseText("<gray>(unshaped)"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(ROPE_LENGTH_KEY, PersistentDataType.INTEGER, meters);

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Creates a combined rope coil placeholder for the recipe book.
     * Name: "Combined Rope Coil", Lore: "Combine any two rope coils"
     */
    public ItemStack createCombinedCoilForRecipe() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        applyTextureFromBase64(meta, plugin.getConfiguration().getHeadTexture());

        meta.displayName(parseText("<gold>Combined Rope Coil"));
        meta.lore(List.of(parseText("<gray>Combine any two rope coils")));

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Creates a rope arrow placeholder for the recipe book.
     * Name: "Rope Arrow" (no length), Lore explains it can be crafted with any rope coil.
     */
    public ItemStack createRopeArrowForRecipe() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();

        meta.displayName(parseText("<gold>Rope Arrow"));
        meta.lore(List.of(
            parseText("<gray>Places a rope when it lands"),
            parseText("<gray>Crafted with a rope coil of any length")
        ));

        if (plugin.getConfiguration().isRopeArrowGlint()) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

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
