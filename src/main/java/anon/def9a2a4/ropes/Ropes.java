package anon.def9a2a4.ropes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

/**
 * Core rope placement, breaking, and detection logic.
 */
public class Ropes {
    private final RopesPlugin plugin;
    private final Display display;

    public Ropes(RopesPlugin plugin, Display display) {
        this.plugin = plugin;
        this.display = display;
    }

    /**
     * Places a rope hanging down from the anchor location.
     *
     * @param anchor The location to start placing rope from (will be the topmost rope block)
     * @param length The desired length in meters/blocks
     * @return The actual number of blocks placed (may be less if obstructed)
     */
    public int placeRope(Location anchor, int length) {
        World world = anchor.getWorld();
        if (world == null) return 0;

        Material chainMaterial = plugin.getConfiguration().getChainMaterial();
        int placed = 0;

        for (int i = 0; i < length; i++) {
            Location loc = anchor.clone().subtract(0, i, 0);
            Block block = loc.getBlock();

            // Check world boundaries
            if (loc.getY() < world.getMinHeight()) {
                break;
            }

            // Check if we can place here (air or replaceable)
            if (!block.isEmpty() && !block.isLiquid() && !block.isReplaceable()) {
                break;
            }

            // Check if chunk is loaded
            if (!world.isChunkLoaded(block.getChunk())) {
                break;
            }

            // Place chain block and display entity
            block.setType(chainMaterial);
            display.spawnRopeDisplay(loc);
            placed++;
        }

        return placed;
    }

    /**
     * Places a rope with animation, one block at a time from top to bottom.
     *
     * @param anchor The location to start placing rope from (will be the topmost rope block)
     * @param length The desired length in meters/blocks
     * @param onComplete Callback invoked when placement finishes, receives the actual number of blocks placed
     */
    public void placeRopeAnimated(Location anchor, int length, Consumer<Integer> onComplete) {
        World world = anchor.getWorld();
        if (world == null) {
            if (onComplete != null) onComplete.accept(0);
            return;
        }

        Material chainMaterial = plugin.getConfiguration().getChainMaterial();
        int ticksPerBlock = plugin.getConfiguration().getAnimationTicksPerBlock();

        new BukkitRunnable() {
            int currentIndex = 0;
            int placed = 0;

            @Override
            public void run() {
                try {
                    if (currentIndex >= length) {
                        cancel();
                        if (onComplete != null) onComplete.accept(placed);
                        return;
                    }

                    Location loc = anchor.clone().subtract(0, currentIndex, 0);
                    Block block = loc.getBlock();

                    // Check world boundaries
                    if (loc.getY() < world.getMinHeight()) {
                        cancel();
                        if (onComplete != null) onComplete.accept(placed);
                        return;
                    }

                    // Check if we can place here (air or replaceable)
                    if (!block.isEmpty() && !block.isLiquid() && !block.isReplaceable()) {
                        cancel();
                        if (onComplete != null) onComplete.accept(placed);
                        return;
                    }

                    // Check if chunk is loaded
                    if (!world.isChunkLoaded(block.getChunk())) {
                        cancel();
                        if (onComplete != null) onComplete.accept(placed);
                        return;
                    }

                    // Place chain block and display entity
                    block.setType(chainMaterial);
                    display.spawnRopeDisplay(loc);
                    placed++;
                    currentIndex++;
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during animated rope placement: " + e.getMessage());
                    cancel();
                    if (onComplete != null) onComplete.accept(placed);
                }
            }
        }.runTaskTimer(plugin, 0, ticksPerBlock);
    }

    /**
     * Breaks an entire rope from any block in it.
     * Removes all chain blocks and display entities.
     *
     * @param anyRopeBlock Any location within the rope
     * @return The total length of the rope that was broken
     */
    public int breakRope(Location anyRopeBlock) {
        Location anchor = findRopeAnchor(anyRopeBlock);
        if (anchor == null) return 0;

        World world = anchor.getWorld();
        if (world == null) return 0;

        int length = 0;
        Location current = anchor.clone();

        // Remove rope blocks going downward
        while (isRopeBlock(current)) {
            Block block = current.getBlock();
            display.removeRopeDisplay(current);
            block.setType(Material.AIR);
            length++;
            current.subtract(0, 1, 0);
        }

        return length;
    }

    /**
     * Extends an existing rope from its bottom.
     *
     * @param bottomBlock The bottom block of the existing rope
     * @param additionalLength How many meters to add
     * @return The actual number of blocks added
     */
    public int extendRope(Location bottomBlock, int additionalLength) {
        // Find the actual bottom by going down until we hit non-rope
        Location current = bottomBlock.clone();
        while (isRopeBlock(current.clone().subtract(0, 1, 0))) {
            current.subtract(0, 1, 0);
        }

        // Start placing from one block below the current bottom
        Location placeStart = current.clone().subtract(0, 1, 0);
        return placeRope(placeStart, additionalLength);
    }

    /**
     * Extends an existing rope from its bottom with animation.
     *
     * @param bottomBlock The bottom block of the existing rope
     * @param additionalLength How many meters to add
     * @param onComplete Callback invoked when extension finishes, receives the actual number of blocks added
     */
    public void extendRopeAnimated(Location bottomBlock, int additionalLength, Consumer<Integer> onComplete) {
        // Find the actual bottom by going down until we hit non-rope
        Location current = bottomBlock.clone();
        while (isRopeBlock(current.clone().subtract(0, 1, 0))) {
            current.subtract(0, 1, 0);
        }

        // Start placing from one block below the current bottom
        Location placeStart = current.clone().subtract(0, 1, 0);
        placeRopeAnimated(placeStart, additionalLength, onComplete);
    }

    /**
     * Finds the topmost rope block (anchor) from any position in the rope.
     *
     * @param ropeBlock Any location within the rope
     * @return The anchor location, or null if not a rope block
     */
    public Location findRopeAnchor(Location ropeBlock) {
        if (!isRopeBlock(ropeBlock)) return null;

        Location current = ropeBlock.clone();

        // Trace upward until we hit non-rope
        while (isRopeBlock(current.clone().add(0, 1, 0))) {
            current.add(0, 1, 0);
        }

        return current;
    }

    /**
     * Gets the total length of a rope from its anchor.
     *
     * @param anchor The topmost rope block
     * @return The length in meters/blocks
     */
    public int getRopeLength(Location anchor) {
        if (!isRopeBlock(anchor)) return 0;

        int length = 0;
        Location current = anchor.clone();

        while (isRopeBlock(current)) {
            length++;
            current.subtract(0, 1, 0);
        }

        return length;
    }

    /**
     * Checks if a location contains a rope block.
     * A rope block is a chain block with a rope display entity.
     *
     * @param loc The location to check
     * @return true if this is a rope block
     */
    public boolean isRopeBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        Block block = loc.getBlock();
        Material chainMaterial = plugin.getConfiguration().getChainMaterial();

        // Check if block is chain material
        if (block.getType() != chainMaterial) {
            return false;
        }

        // Check for rope display entity
        return display.findRopeDisplay(loc) != null;
    }

    /**
     * Drops rope coils at a location for the given total length.
     * Handles lengths greater than max coil length by dropping multiple coils.
     *
     * @param loc Where to drop the coils
     * @param totalLength Total meters of rope to drop
     */
    public void dropRopeCoils(Location loc, int totalLength) {
        if (totalLength <= 0) return;

        World world = loc.getWorld();
        if (world == null) return;

        int maxLength = plugin.getConfiguration().getRopeCoilMaxLength();
        int remaining = totalLength;

        while (remaining > 0) {
            int coilLength = Math.min(remaining, maxLength);
            ItemStack coil = plugin.getItems().createRopeCoil(coilLength);
            world.dropItemNaturally(loc, coil);
            remaining -= coilLength;
        }
    }

    /**
     * Finds the nearest rope block within the given radius of a location.
     *
     * @param center The center location to search from
     * @param radius The search radius in blocks
     * @return The nearest rope block location, or null if none found
     */
    public Location findNearestRope(Location center, double radius) {
        if (center == null || center.getWorld() == null) return null;

        Location nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        int searchRadius = (int) Math.ceil(radius);
        Block centerBlock = center.getBlock();

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    Block checkBlock = centerBlock.getRelative(dx, dy, dz);
                    Location checkLoc = checkBlock.getLocation();

                    Location blockCenter = checkLoc.clone().add(0.5, 0.5, 0.5);
                    double distSq = center.distanceSquared(blockCenter);

                    if (distSq <= radius * radius && isRopeBlock(checkLoc)) {
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            nearest = checkLoc;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * Gets the bottom-most block of a rope.
     *
     * @param anyRopeBlock Any location within the rope
     * @return The bottom rope block location, or null if not a rope
     */
    public Location findRopeBottom(Location anyRopeBlock) {
        if (!isRopeBlock(anyRopeBlock)) return null;

        Location current = anyRopeBlock.clone();

        // Trace downward until we hit non-rope
        while (isRopeBlock(current.clone().subtract(0, 1, 0))) {
            current.subtract(0, 1, 0);
        }

        return current;
    }
}
