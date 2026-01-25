package anon.def9a2a4.ropes;

import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class RopesPlugin extends JavaPlugin {
    private static RopesPlugin instance;
    private Config configuration;
    private Items items;
    private Display display;
    private Ropes ropes;

    @Override
    public void onEnable() {
        instance = this;

        // Load configuration
        configuration = new Config(this);

        // Initialize items manager
        items = new Items(this);

        // Initialize display and ropes managers
        display = new Display(this);
        ropes = new Ropes(this, display);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        // Register commands
        Commands commands = new Commands(this);
        getCommand("ropes").setExecutor(commands);
        getCommand("ropes").setTabCompleter(commands);

        // Register crafting recipes
        registerRecipes();

        // Initialize bStats
        new Metrics(this, 29032);

        getLogger().info("Ropes plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Ropes plugin disabled!");
    }

    private void registerRecipes() {
        // Rope Coil Recipe
        Config.RecipeConfig coilConfig = configuration.getRopeCoilRecipeConfig();
        if (coilConfig.enabled()) {
            registerRopeCoilRecipe(coilConfig);
        }

        // Rope Coil Combine Recipe: 2 rope coils -> combined coil
        // Note: Uses MaterialChoice to allow any rope coil length. Actual combining logic
        // with correct length is handled in PrepareItemCraftEvent (Listeners)
        if (configuration.isRopeCoilCombineEnabled()) {
            ShapelessRecipe combineRecipe = new ShapelessRecipe(
                new NamespacedKey(this, "rope_coil_combine"),
                items.createCombinedCoilForRecipe()
            );
            // Use MaterialChoice so any player head (rope coil) works, validation in Listeners
            combineRecipe.addIngredient(new RecipeChoice.MaterialChoice(Material.PLAYER_HEAD));
            combineRecipe.addIngredient(new RecipeChoice.MaterialChoice(Material.PLAYER_HEAD));
            combineRecipe.setCategory(CraftingBookCategory.EQUIPMENT);
            getServer().addRecipe(combineRecipe);
        }

        // Rope Arrow Recipe
        Config.RecipeConfig arrowConfig = configuration.getRopeArrowRecipeConfig();
        if (arrowConfig.enabled()) {
            registerRopeArrowRecipe(arrowConfig);
        }
    }

    private void registerRopeCoilRecipe(Config.RecipeConfig config) {
        NamespacedKey key = new NamespacedKey(this, "rope_coil");
        var result = items.createRopeCoilForRecipe(configuration.getRopeCoilDefaultLength());

        if (config.type() == Config.RecipeType.SHAPED) {
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(config.pattern().toArray(new String[0]));

            for (Map.Entry<Character, Material> entry : config.shapedIngredients().entrySet()) {
                if (entry.getValue() != null) {
                    recipe.setIngredient(entry.getKey(), entry.getValue());
                }
            }
            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            getServer().addRecipe(recipe);
        } else {
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            for (Config.IngredientConfig ing : config.shapelessIngredients()) {
                if (ing.material() != null) {
                    recipe.addIngredient(ing.amount(), ing.material());
                }
            }
            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            getServer().addRecipe(recipe);
        }
    }

    private void registerRopeArrowRecipe(Config.RecipeConfig config) {
        NamespacedKey key = new NamespacedKey(this, "rope_arrow");
        var result = items.createRopeArrowForRecipe();

        if (config.type() == Config.RecipeType.SHAPED) {
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(config.pattern().toArray(new String[0]));

            for (Map.Entry<Character, Material> entry : config.shapedIngredients().entrySet()) {
                if (entry.getValue() == null) {
                    // Rope coil ingredient - use MaterialChoice so any rope coil works
                    // Actual validation is done in PrepareItemCraftEvent listener
                    recipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(Material.PLAYER_HEAD));
                } else {
                    recipe.setIngredient(entry.getKey(), entry.getValue());
                }
            }
            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            getServer().addRecipe(recipe);
        } else {
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            for (Config.IngredientConfig ing : config.shapelessIngredients()) {
                if (ing.material() == null) {
                    // Rope coil ingredient - use MaterialChoice so any rope coil works
                    // Actual validation is done in PrepareItemCraftEvent listener
                    for (int i = 0; i < ing.amount(); i++) {
                        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.PLAYER_HEAD));
                    }
                } else {
                    recipe.addIngredient(ing.amount(), ing.material());
                }
            }
            recipe.setCategory(CraftingBookCategory.EQUIPMENT);
            getServer().addRecipe(recipe);
        }
    }

    public static RopesPlugin getInstance() {
        return instance;
    }

    public Config getConfiguration() {
        return configuration;
    }

    public Items getItems() {
        return items;
    }

    public Display getDisplay() {
        return display;
    }

    public Ropes getRopes() {
        return ropes;
    }
}
