package anon.def9a2a4.ropes;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Config {
    private final RopesPlugin plugin;

    // Rope Coil Settings
    private int ropeCoilDefaultLength;
    private int ropeCoilMaxLength;
    private String headTexture;
    private ItemDisplayConfig ropeCoilItemConfig;

    // Rope Block Settings
    private Material chainMaterial;
    private double climbSpeed;
    private double climbVelocityThreshold;
    private int interactionRadius;
    private Set<Material> anchorFences;
    private String ropeBlockDisplayTexture;
    private DisplayScale displayScale;
    private float displayOffsetY;
    private boolean animationEnabled;
    private int animationTicksPerBlock;

    // Rope Arrow Settings
    private Material fenceMaterial;
    private double ropeArrowExtendRadius;
    private boolean ropeArrowGlint;
    private Material arrowImpactParticleMaterial;
    private ItemDisplayConfig ropeArrowItemConfig;

    // Recipe Configs
    private RecipeConfig ropeCoilRecipeConfig;
    private boolean ropeCoilCombineEnabled;
    private RecipeConfig ropeArrowRecipeConfig;
    private String recipeUnlockAdvancement;

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

        // Rope Coil Item Config
        String coilName = config.getString("rope-coil.item.name", "<gold>Rope Coil");
        List<String> coilLore = config.getStringList("rope-coil.item.lore");
        if (coilLore.isEmpty()) {
            coilLore = List.of("<gray><meters> meters of rope");
        }
        ropeCoilItemConfig = new ItemDisplayConfig(coilName, coilLore);

        // Rope Block Settings
        String chainMaterialName = config.getString("rope-block.chain-material", "WAXED_EXPOSED_COPPER_CHAIN");
        chainMaterial = Material.matchMaterial(chainMaterialName);
        if (chainMaterial == null) {
            chainMaterial = Material.matchMaterial("WAXED_EXPOSED_COPPER_CHAIN");
            if (chainMaterial == null) {
                chainMaterial = Material.CHAIN;
            }
            plugin.getLogger().warning("Invalid chain material: " + chainMaterialName + ", using " + chainMaterial.name());
        }
        climbSpeed = config.getDouble("rope-block.climb-speed", 0.2);
        climbVelocityThreshold = config.getDouble("rope-block.climb-velocity-threshold", 0.05);
        interactionRadius = config.getInt("rope-block.interaction-radius", 1);

        // Anchor Fences
        anchorFences = EnumSet.noneOf(Material.class);
        List<String> fenceList = config.getStringList("rope-block.anchor-fences");
        for (String fenceName : fenceList) {
            Material mat = Material.matchMaterial(fenceName);
            if (mat != null) {
                anchorFences.add(mat);
            } else {
                plugin.getLogger().warning("Invalid anchor fence material: " + fenceName);
            }
        }

        ropeBlockDisplayTexture = config.getString("rope-block.display-texture", headTexture);

        // Display Scale
        double scaleX = config.getDouble("rope-block.display-scale.x", 0.2);
        double scaleY = config.getDouble("rope-block.display-scale.y", 2.0);
        double scaleZ = config.getDouble("rope-block.display-scale.z", 0.2);
        if (scaleX <= 0 || scaleY <= 0 || scaleZ <= 0) {
            plugin.getLogger().warning("Invalid display scale values, using defaults");
            displayScale = new DisplayScale(0.2f, 2.0f, 0.2f);
        } else {
            displayScale = new DisplayScale((float) scaleX, (float) scaleY, (float) scaleZ);
        }

        // Display Offset Y
        displayOffsetY = (float) config.getDouble("rope-block.display-offset-y", 0.0);

        // Animation Settings
        animationEnabled = config.getBoolean("rope-block.animation.enabled", true);
        animationTicksPerBlock = config.getInt("rope-block.animation.ticks-per-block", 2);
        if (animationTicksPerBlock < 1) {
            plugin.getLogger().warning("Invalid animation ticks-per-block value, using default of 2");
            animationTicksPerBlock = 2;
        }

        // Rope Arrow Settings
        String fenceMaterialName = config.getString("rope-arrow.place-material", "OAK_FENCE");
        fenceMaterial = Material.matchMaterial(fenceMaterialName);
        if (fenceMaterial == null) {
            plugin.getLogger().warning("Invalid fence material: " + fenceMaterialName + ", using OAK_FENCE");
            fenceMaterial = Material.OAK_FENCE;
        }
        ropeArrowExtendRadius = config.getDouble("rope-arrow.extend-radius", 0.5);
        ropeArrowGlint = config.getBoolean("rope-arrow.glint", true);

        String particleMaterialName = config.getString("rope-arrow.impact-particle-material", "OAK_PLANKS");
        arrowImpactParticleMaterial = Material.matchMaterial(particleMaterialName);
        if (arrowImpactParticleMaterial == null || !arrowImpactParticleMaterial.isBlock()) {
            plugin.getLogger().warning("Invalid impact particle material: " + particleMaterialName + ", using OAK_PLANKS");
            arrowImpactParticleMaterial = Material.OAK_PLANKS;
        }

        // Rope Arrow Item Config
        String arrowName = config.getString("rope-arrow.item.name", "<gold>Rope Arrow");
        List<String> arrowLore = config.getStringList("rope-arrow.item.lore");
        if (arrowLore.isEmpty()) {
            arrowLore = List.of("<gray>Shoots a rope where it lands", "<dark_gray><meters> meters of rope");
        }
        ropeArrowItemConfig = new ItemDisplayConfig(arrowName, arrowLore);

        // Recipe Configs
        ropeCoilRecipeConfig = loadRecipeConfig(config, "recipes.rope-coil", true);
        ropeCoilCombineEnabled = config.getBoolean("recipes.rope-coil-combine.enabled", true);
        ropeArrowRecipeConfig = loadRecipeConfig(config, "recipes.rope-arrow", true);
        recipeUnlockAdvancement = config.getString("recipes.unlock-on-advancement", "minecraft:adventure/ol_betsy");
    }

    private RecipeConfig loadRecipeConfig(FileConfiguration config, String path, boolean defaultEnabled) {
        boolean enabled = config.getBoolean(path + ".enabled", defaultEnabled);
        if (!enabled) {
            return new RecipeConfig(false, RecipeType.SHAPELESS, null, null, null, false);
        }

        String typeStr = config.getString(path + ".type", "shapeless");
        RecipeType type = typeStr.equalsIgnoreCase("shaped") ? RecipeType.SHAPED : RecipeType.SHAPELESS;

        List<String> pattern = null;
        Map<Character, Material> shapedIngredients = null;
        List<IngredientConfig> shapelessIngredients = null;
        boolean hasRopeCoilIngredient = false;

        if (type == RecipeType.SHAPED) {
            pattern = config.getStringList(path + ".pattern");
            if (pattern.isEmpty()) {
                plugin.getLogger().warning("No pattern defined for shaped recipe at " + path);
            }

            shapedIngredients = new HashMap<>();
            ConfigurationSection ingredientSection = config.getConfigurationSection(path + ".ingredients");
            if (ingredientSection != null) {
                for (String key : ingredientSection.getKeys(false)) {
                    if (key.length() == 1) {
                        String matName = ingredientSection.getString(key);
                        if ("ROPE_COIL".equalsIgnoreCase(matName)) {
                            hasRopeCoilIngredient = true;
                            shapedIngredients.put(key.charAt(0), null);
                        } else {
                            Material mat = Material.matchMaterial(matName);
                            if (mat != null) {
                                shapedIngredients.put(key.charAt(0), mat);
                            } else {
                                plugin.getLogger().warning("Invalid material '" + matName + "' for ingredient '" + key + "' at " + path);
                            }
                        }
                    }
                }
            }
        } else {
            shapelessIngredients = new ArrayList<>();
            List<Map<?, ?>> ingredientList = config.getMapList(path + ".ingredients");
            for (Map<?, ?> ing : ingredientList) {
                String matName = String.valueOf(ing.get("material"));
                int amount = ing.containsKey("amount") ? ((Number) ing.get("amount")).intValue() : 1;

                if ("ROPE_COIL".equalsIgnoreCase(matName)) {
                    hasRopeCoilIngredient = true;
                    shapelessIngredients.add(new IngredientConfig(null, amount));
                } else {
                    Material mat = Material.matchMaterial(matName);
                    if (mat != null) {
                        shapelessIngredients.add(new IngredientConfig(mat, amount));
                    } else {
                        plugin.getLogger().warning("Invalid material '" + matName + "' at " + path);
                    }
                }
            }
        }

        return new RecipeConfig(enabled, type, pattern, shapedIngredients, shapelessIngredients, hasRopeCoilIngredient);
    }

    // Getters

    public int getRopeCoilDefaultLength() {
        return ropeCoilDefaultLength;
    }

    public int getRopeCoilMaxLength() {
        return ropeCoilMaxLength;
    }

    public String getHeadTexture() {
        return headTexture;
    }

    public ItemDisplayConfig getRopeCoilItemConfig() {
        return ropeCoilItemConfig;
    }

    public Material getChainMaterial() {
        return chainMaterial;
    }

    public double getClimbSpeed() {
        return climbSpeed;
    }

    public double getClimbVelocityThreshold() {
        return climbVelocityThreshold;
    }

    public int getInteractionRadius() {
        return interactionRadius;
    }

    public Set<Material> getAnchorFences() {
        return anchorFences;
    }

    public boolean isAnchorFence(Material material) {
        return anchorFences.contains(material);
    }

    public String getRopeBlockDisplayTexture() {
        return ropeBlockDisplayTexture;
    }

    public DisplayScale getDisplayScale() {
        return displayScale;
    }

    public float getDisplayOffsetY() {
        return displayOffsetY;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public int getAnimationTicksPerBlock() {
        return animationTicksPerBlock;
    }

    public Material getFenceMaterial() {
        return fenceMaterial;
    }

    public double getRopeArrowExtendRadius() {
        return ropeArrowExtendRadius;
    }

    public boolean isRopeArrowGlint() {
        return ropeArrowGlint;
    }

    public Material getArrowImpactParticleMaterial() {
        return arrowImpactParticleMaterial;
    }

    public ItemDisplayConfig getRopeArrowItemConfig() {
        return ropeArrowItemConfig;
    }

    public RecipeConfig getRopeCoilRecipeConfig() {
        return ropeCoilRecipeConfig;
    }

    public boolean isRopeCoilCombineEnabled() {
        return ropeCoilCombineEnabled;
    }

    public RecipeConfig getRopeArrowRecipeConfig() {
        return ropeArrowRecipeConfig;
    }

    public String getRecipeUnlockAdvancement() {
        return recipeUnlockAdvancement;
    }

    // Inner Records and Enums

    public record DisplayScale(float x, float y, float z) {
        public Vector3f toVector3f() {
            return new Vector3f(x, y, z);
        }
    }

    public record ItemDisplayConfig(String nameTemplate, List<String> loreTemplates) {}

    public record RecipeConfig(
        boolean enabled,
        RecipeType type,
        List<String> pattern,
        Map<Character, Material> shapedIngredients,
        List<IngredientConfig> shapelessIngredients,
        boolean hasRopeCoilIngredient
    ) {}

    public enum RecipeType {
        SHAPED, SHAPELESS
    }

    public record IngredientConfig(Material material, int amount) {}
}
