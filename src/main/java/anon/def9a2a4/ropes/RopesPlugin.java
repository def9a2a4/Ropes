package anon.def9a2a4.ropes;

import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

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

        // Register crafting recipes
        registerRecipes();

        // Initialize bStats
        new Metrics(this, 12345); // TODO: Replace with actual bStats plugin ID

        getLogger().info("Ropes plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Ropes plugin disabled!");
    }

    private void registerRecipes() {
        // Rope Coil Recipe: 6 string -> rope coil (2m)
        if (configuration.isRopeCoilRecipeEnabled()) {
            ShapelessRecipe ropeCoilRecipe = new ShapelessRecipe(
                new NamespacedKey(this, "rope_coil"),
                items.createRopeCoil(configuration.getRopeCoilDefaultLength())
            );
            ropeCoilRecipe.addIngredient(6, Material.STRING);
            getServer().addRecipe(ropeCoilRecipe);
        }

        // Rope Coil Combine Recipe: 2 rope coils -> combined coil
        // Note: Actual combining logic with correct length is handled in PrepareItemCraftEvent (Listeners)
        if (configuration.isRopeCoilCombineEnabled()) {
            ShapelessRecipe combineRecipe = new ShapelessRecipe(
                new NamespacedKey(this, "rope_coil_combine"),
                items.createRopeCoil(configuration.getRopeCoilDefaultLength() * 2)
            );
            combineRecipe.addIngredient(new RecipeChoice.ExactChoice(
                items.createRopeCoil(configuration.getRopeCoilDefaultLength())
            ));
            combineRecipe.addIngredient(new RecipeChoice.ExactChoice(
                items.createRopeCoil(configuration.getRopeCoilDefaultLength())
            ));
            getServer().addRecipe(combineRecipe);
        }

        // Rope Arrow Recipe:
        // - A -
        // - S -
        // S R S
        // Where A=arrow, S=stick, R=rope coil
        if (configuration.isRopeArrowRecipeEnabled()) {
            ShapedRecipe ropeArrowRecipe = new ShapedRecipe(
                new NamespacedKey(this, "rope_arrow"),
                items.createRopeArrow(configuration.getRopeCoilDefaultLength())
            );
            ropeArrowRecipe.shape(" A ", " S ", "SRS");
            ropeArrowRecipe.setIngredient('A', Material.ARROW);
            ropeArrowRecipe.setIngredient('S', Material.STICK);
            ropeArrowRecipe.setIngredient('R', new RecipeChoice.ExactChoice(
                items.createRopeCoil(configuration.getRopeCoilDefaultLength())
            ));
            getServer().addRecipe(ropeArrowRecipe);
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
