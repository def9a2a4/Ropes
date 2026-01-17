package anon.def9a2a4.ropes;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.UUID;

public class Display {
    public static final String ROPE_DISPLAY_TAG = "ropes_display";

    private final RopesPlugin plugin;

    public Display(RopesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a rope display entity at the given location.
     * Scale and Y offset are configurable in config.yml.
     */
    public ItemDisplay spawnRopeDisplay(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        // Center the display in the block
        Location spawnLoc = loc.getBlock().getLocation().add(0.5, 0.5, 0.5);

        // Get configurable scale and offset
        Config config = plugin.getConfiguration();
        Vector3f scale = config.getDisplayScale().toVector3f();
        float offsetY = config.getDisplayOffsetY();

        return world.spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(createRopeDisplayItem());
            display.setTransformation(new Transformation(
                new Vector3f(0, offsetY, 0),    // translation with configurable Y offset
                new AxisAngle4f(0, 0, 0, 1),    // left rotation
                scale,                          // configurable scale
                new AxisAngle4f(0, 0, 0, 1)     // right rotation
            ));
            display.addScoreboardTag(ROPE_DISPLAY_TAG);
        });
    }

    /**
     * Removes the rope display entity at the given location.
     * Returns true if a display was found and removed.
     */
    public boolean removeRopeDisplay(Location loc) {
        ItemDisplay display = findRopeDisplay(loc);
        if (display != null) {
            display.remove();
            return true;
        }
        return false;
    }

    /**
     * Finds the rope display entity at the given block location.
     * Returns null if no rope display exists there.
     */
    public ItemDisplay findRopeDisplay(Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;

        // Search for entities in a small radius around the block center
        Location center = loc.getBlock().getLocation().add(0.5, 0.5, 0.5);
        Collection<Entity> nearby = world.getNearbyEntities(center, 0.6, 0.6, 0.6);

        for (Entity entity : nearby) {
            if (isRopeDisplay(entity)) {
                return (ItemDisplay) entity;
            }
        }
        return null;
    }

    /**
     * Checks if an entity is a rope display entity.
     */
    public boolean isRopeDisplay(Entity entity) {
        return entity instanceof ItemDisplay && entity.getScoreboardTags().contains(ROPE_DISPLAY_TAG);
    }

    /**
     * Checks if there is a rope display at the given block location.
     */
    public boolean hasRopeDisplayAt(Location loc) {
        return findRopeDisplay(loc) != null;
    }

    /**
     * Removes all rope display entities at the given location.
     * Handles edge case of multiple displays at same location.
     * Returns the count of removed displays.
     */
    public int removeAllRopeDisplaysAt(Location loc) {
        World world = loc.getWorld();
        if (world == null) return 0;

        Location center = loc.getBlock().getLocation().add(0.5, 0.5, 0.5);
        Collection<Entity> nearby = world.getNearbyEntities(center, 0.6, 0.6, 0.6);

        int removed = 0;
        for (Entity entity : nearby) {
            if (isRopeDisplay(entity)) {
                entity.remove();
                removed++;
            }
        }
        return removed;
    }

    /**
     * Creates the item stack used for rope display entities.
     * This is a player head with the configured rope texture.
     */
    private ItemStack createRopeDisplayItem() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", plugin.getConfiguration().getRopeBlockDisplayTexture()));
        meta.setPlayerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }
}
