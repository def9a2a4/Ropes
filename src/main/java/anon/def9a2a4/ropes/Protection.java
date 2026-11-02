package anon.def9a2a4.ropes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Lets protection plugins (Towny, WorldGuard, GriefPrevention, ...) veto rope changes.
 * Ropes sets blocks directly, so without these checks no plugin ever sees the player's action.
 */
public final class Protection {
    private Protection() {}

    /**
     * Sets the block to the given type on behalf of the player, the way vanilla does: place it,
     * fire a BlockPlaceEvent, and restore the old block if a plugin cancels.
     *
     * @param player who is placing, or null to place without a check (no player to attribute it to)
     * @return true if the block was placed
     */
    public static boolean place(Block block, Material type, Block placedAgainst, Player player) {
        if (player == null) {
            block.setType(type);
            return true;
        }

        // Set without physics so a refused placement leaves nothing behind (neighbours, observers, plants)
        BlockState replaced = block.getState();
        block.setType(type, false);

        BlockPlaceEvent event = new BlockPlaceEvent(block, replaced, placedAgainst,
            new ItemStack(type), player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || !event.canBuild()) {
            replaced.update(true, false);
            return false;
        }

        // Allowed: put the old block back quietly, then place for real so physics run
        replaced.update(true, false);
        block.setType(type, true);
        return true;
    }

    /**
     * Asks plugins whether the player may break the block, without breaking it.
     *
     * @param player who is breaking, or null to allow
     */
    public static boolean canBreak(Block block, Player player) {
        if (player == null) return true;
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        event.setDropItems(false);
        event.setExpToDrop(0);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
