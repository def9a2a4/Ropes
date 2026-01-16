package anon.def9a2a4.ropes;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    private final RopesPlugin plugin;

    // Rope Coil Settings
    private int ropeCoilDefaultLength;
    private int ropeCoilMaxLength;
    private String headTexture;

    // Rope Block Settings
    private Material chainMaterial;
    private double climbSpeed;

    // Rope Arrow Settings
    private Material fenceMaterial;
    private boolean ropeArrowGlint;

    // Recipe Toggles
    private boolean ropeCoilRecipeEnabled;
    private boolean ropeCoilCombineEnabled;
    private boolean ropeArrowRecipeEnabled;

    public Config(RopesPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Rope Coil Settings
        ropeCoilDefaultLength = config.getInt("rope-coil.default-length", 2);
        ropeCoilMaxLength = config.getInt("rope-coil.max-length", 16);
        headTexture = config.getString("rope-coil.head-texture",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGY1MTM2MWE4MGM3MmQ2ODUwN2E0YzVkY2I3ZDY3MWNmMGZmZGMzNTc3YmNlOWU3OWVmOWFmOTMxNTM2YmE1MyJ9fX0=");

        // Rope Block Settings
        String chainMaterialName = config.getString("rope-block.chain-material", "WAXED_EXPOSED_COPPER_CHAIN");
        chainMaterial = Material.matchMaterial(chainMaterialName);
        if (chainMaterial == null) {
            // Try waxed exposed copper chain first, fall back to regular chain
            chainMaterial = Material.matchMaterial("WAXED_EXPOSED_COPPER_CHAIN");
            if (chainMaterial == null) {
                chainMaterial = Material.CHAIN;
            }
            plugin.getLogger().warning("Invalid chain material: " + chainMaterialName + ", using " + chainMaterial.name());
        }
        climbSpeed = config.getDouble("rope-block.climb-speed", 0.2);

        // Rope Arrow Settings
        String fenceMaterialName = config.getString("rope-arrow.fence-material", "OAK_FENCE");
        fenceMaterial = Material.matchMaterial(fenceMaterialName);
        if (fenceMaterial == null) {
            plugin.getLogger().warning("Invalid fence material: " + fenceMaterialName + ", using OAK_FENCE");
            fenceMaterial = Material.OAK_FENCE;
        }
        ropeArrowGlint = config.getBoolean("rope-arrow.glint", true);

        // Recipe Toggles
        ropeCoilRecipeEnabled = config.getBoolean("recipes.rope-coil-enabled", true);
        ropeCoilCombineEnabled = config.getBoolean("recipes.rope-coil-combine-enabled", true);
        ropeArrowRecipeEnabled = config.getBoolean("recipes.rope-arrow-enabled", true);
    }

    public int getRopeCoilDefaultLength() {
        return ropeCoilDefaultLength;
    }

    public int getRopeCoilMaxLength() {
        return ropeCoilMaxLength;
    }

    public String getHeadTexture() {
        return headTexture;
    }

    public Material getChainMaterial() {
        return chainMaterial;
    }

    public double getClimbSpeed() {
        return climbSpeed;
    }

    public Material getFenceMaterial() {
        return fenceMaterial;
    }

    public boolean isRopeArrowGlint() {
        return ropeArrowGlint;
    }

    public boolean isRopeCoilRecipeEnabled() {
        return ropeCoilRecipeEnabled;
    }

    public boolean isRopeCoilCombineEnabled() {
        return ropeCoilCombineEnabled;
    }

    public boolean isRopeArrowRecipeEnabled() {
        return ropeArrowRecipeEnabled;
    }
}
