package anon.def9a2a4.ropes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Listeners implements Listener {
    private final RopesPlugin plugin;
    private final Items items;
    private final Ropes ropes;
    private final Config config;

    public Listeners(RopesPlugin plugin) {
        this.plugin = plugin;
        this.items = plugin.getItems();
        this.ropes = plugin.getRopes();
        this.config = plugin.getConfiguration();
    }

    // ==================== CRAFTING ====================

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null) return;

        ItemStack[] matrix = inv.getMatrix();

        // Check for rope coil combine recipe (result is player head)
        if (result.getType() == Material.PLAYER_HEAD) {
            List<ItemStack> coils = new ArrayList<>();
            for (ItemStack item : matrix) {
                if (item != null && items.isRopeCoil(item)) {
                    coils.add(item);
                }
            }

            // If we have exactly 2 rope coils, this is a combine recipe
            if (coils.size() == 2) {
                ItemStack combined = items.combineCoils(coils.get(0), coils.get(1));
                inv.setResult(combined);
            } else if (coils.size() == 1) {
                // Single coil with other player heads that aren't coils - invalid
                // Check if there are other player heads
                int playerHeadCount = 0;
                for (ItemStack item : matrix) {
                    if (item != null && item.getType() == Material.PLAYER_HEAD) {
                        playerHeadCount++;
                    }
                }
                if (playerHeadCount > 1) {
                    // Mixed player heads - cancel
                    inv.setResult(null);
                }
            } else if (coils.isEmpty()) {
                // No coils but result is player head - might be a different recipe
                // Check if any player heads in matrix aren't coils
                boolean hasNonCoilHead = false;
                for (ItemStack item : matrix) {
                    if (item != null && item.getType() == Material.PLAYER_HEAD && !items.isRopeCoil(item)) {
                        hasNonCoilHead = true;
                        break;
                    }
                }
                if (hasNonCoilHead) {
                    // This is some other player head recipe, not ours
                    return;
                }
            }
        }

        // Check for rope arrow recipe (result is arrow)
        if (result.getType() == Material.ARROW && items.isRopeArrow(result)) {
            // Find the rope coil in the matrix to get its length
            for (ItemStack item : matrix) {
                if (item != null && items.isRopeCoil(item)) {
                    int ropeLength = items.getRopeLength(item);
                    inv.setResult(items.createRopeArrow(ropeLength));
                    break;
                }
            }
        }
    }

    // ==================== PLAYER INTERACT ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Player player = event.getPlayer();
        Location blockLoc = clickedBlock.getLocation();

        // LEFT CLICK - Break rope
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (ropes.isRopeBlock(blockLoc)) {
                event.setCancelled(true);

                int length = ropes.breakRope(blockLoc);
                if (length > 0) {
                    ropes.dropRopeCoils(blockLoc.add(0.5, 0.5, 0.5), length);
                }
            }
            return;
        }

        // RIGHT CLICK - Place or extend rope
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (!items.isRopeCoil(itemInHand)) return;

            // Cancel event immediately to prevent rope coil from being placed as a block
            event.setCancelled(true);

            int coilLength = items.getRopeLength(itemInHand);

            // Clicked on existing rope - extend it
            if (ropes.isRopeBlock(blockLoc)) {
                int added = ropes.extendRope(blockLoc, coilLength);
                if (added > 0) {
                    // Consume the coil
                    consumeItemInHand(player);

                    // Refund unused rope
                    int unused = coilLength - added;
                    if (unused > 0) {
                        ItemStack refund = items.createRopeCoil(unused);
                        player.getInventory().addItem(refund).values()
                            .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                    }
                }
                return;
            }

            // Clicked on solid block - check if we can place rope below
            BlockFace face = event.getBlockFace();
            Block targetBlock;

            if (face == BlockFace.DOWN) {
                // Clicked on bottom of a block - place below clicked block
                targetBlock = clickedBlock.getRelative(BlockFace.DOWN);
            } else if (face == BlockFace.UP) {
                // Clicked on top of a block - that block is the anchor, place below
                targetBlock = clickedBlock;
            } else {
                // Clicked on side - place on the side
                targetBlock = clickedBlock.getRelative(face);
            }

            // Special case: clicking on top of a fence - the fence itself is the anchor
            if (face == BlockFace.UP && config.isAnchorFence(clickedBlock.getType())) {
                targetBlock = clickedBlock.getRelative(BlockFace.DOWN);
            }

            // Need a solid block above the placement location
            Block anchorBlock = targetBlock.getRelative(BlockFace.UP);
            boolean validAnchor = anchorBlock.getType().isSolid();

            // Special case: fence can anchor if target is air/replaceable or rope
            if (!validAnchor && config.isAnchorFence(anchorBlock.getType())) {
                if (targetBlock.isEmpty() || targetBlock.isReplaceable() || ropes.isRopeBlock(targetBlock.getLocation())) {
                    validAnchor = true;
                }
            }

            if (!validAnchor) return;

            // If placing below a fence that already has rope, extend the existing rope
            if (config.isAnchorFence(anchorBlock.getType()) && ropes.isRopeBlock(targetBlock.getLocation())) {
                int added = ropes.extendRope(targetBlock.getLocation(), coilLength);
                if (added > 0) {
                    consumeItemInHand(player);

                    int unused = coilLength - added;
                    if (unused > 0) {
                        ItemStack refund = items.createRopeCoil(unused);
                        player.getInventory().addItem(refund).values()
                            .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                    }
                }
                return;
            }

            // Check if target is air or replaceable
            if (!targetBlock.isEmpty() && !targetBlock.isReplaceable()) return;

            int placed = ropes.placeRope(targetBlock.getLocation(), coilLength);
            if (placed > 0) {
                // Consume the coil
                consumeItemInHand(player);

                // Refund unused rope
                int unused = coilLength - placed;
                if (unused > 0) {
                    ItemStack refund = items.createRopeCoil(unused);
                    player.getInventory().addItem(refund).values()
                        .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                }
            }
        }
    }

    private void consumeItemInHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    // ==================== BLOCK BREAK - ANCHOR DESTRUCTION ====================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Block belowBlock = brokenBlock.getRelative(BlockFace.DOWN);
        Location belowLoc = belowBlock.getLocation();

        // Check if block below is a rope block
        if (!ropes.isRopeBlock(belowLoc)) return;

        // Check if it's the anchor (no rope block above it)
        Location aboveLoc = belowBlock.getRelative(BlockFace.UP).getLocation();
        if (ropes.isRopeBlock(aboveLoc)) return; // Not the anchor

        // This is the anchor - break the entire rope
        int length = ropes.breakRope(belowLoc);
        if (length > 0) {
            ropes.dropRopeCoils(belowLoc.add(0.5, 0.5, 0.5), length);
        }
    }

    // ==================== EXPLOSIONS ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList(), event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList(), event.getLocation());
    }

    private void handleExplosion(List<Block> blockList, Location explosionLoc) {
        Set<Location> processedAnchors = new HashSet<>();
        List<Block> toRemove = new ArrayList<>();

        for (Block block : blockList) {
            Location loc = block.getLocation();
            if (ropes.isRopeBlock(loc)) {
                toRemove.add(block);

                // Find anchor and process if not already done
                Location anchor = ropes.findRopeAnchor(loc);
                if (anchor != null && !processedAnchors.contains(anchor)) {
                    processedAnchors.add(anchor);
                    int length = ropes.breakRope(loc);
                    if (length > 0) {
                        ropes.dropRopeCoils(anchor.add(0.5, 0.5, 0.5), length);
                    }
                }
            }
        }

        // Remove rope blocks from explosion list to prevent double handling
        blockList.removeAll(toRemove);
    }

    // ==================== PISTONS ====================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePiston(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePiston(event.getBlocks());
    }

    private void handlePiston(List<Block> blocks) {
        Set<Location> processedAnchors = new HashSet<>();

        for (Block block : blocks) {
            Location loc = block.getLocation();
            if (ropes.isRopeBlock(loc)) {
                Location anchor = ropes.findRopeAnchor(loc);
                if (anchor != null && !processedAnchors.contains(anchor)) {
                    processedAnchors.add(anchor);
                    int length = ropes.breakRope(loc);
                    if (length > 0) {
                        ropes.dropRopeCoils(anchor.add(0.5, 0.5, 0.5), length);
                    }
                }
            }
        }
    }

    // ==================== CLIMBING ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();

        // Check if player is on or adjacent to a rope
        if (!isNearRope(playerLoc)) return;

        double climbSpeed = config.getClimbSpeed();
        Vector velocity = player.getVelocity();

        if (player.isSneaking()) {
            // Descend
            velocity.setY(-climbSpeed);
        } else if (velocity.getY() > 0.05) {
            // Player is holding jump (positive Y velocity indicates jump attempt)
            velocity.setY(climbSpeed);
        } else {
            // Hold position - stand still
            velocity.setY(0);
        }

        player.setVelocity(velocity);
        player.setFallDistance(0); // Prevent fall damage while on rope
    }

    private boolean isNearRope(Location loc) {
        int radius = config.getInteractionRadius();
        Block playerBlock = loc.getBlock();

        // Check blocks within Manhattan distance at feet level
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= radius) {
                        Block checkBlock = playerBlock.getRelative(dx, dy, dz);
                        if (ropes.isRopeBlock(checkBlock.getLocation())) return true;
                    }
                }
            }
        }

        // Also check at eye level (1.6 blocks up)
        Block eyeBlock = loc.clone().add(0, 1.6, 0).getBlock();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= radius) {
                        Block checkBlock = eyeBlock.getRelative(dx, dy, dz);
                        if (ropes.isRopeBlock(checkBlock.getLocation())) return true;
                    }
                }
            }
        }

        return false;
    }

    // ==================== ROPE ARROW - SHOOTING ====================

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        ItemStack consumable = event.getConsumable();
        if (consumable == null || !items.isRopeArrow(consumable)) return;

        // Transfer rope length to arrow entity PDC
        int ropeLength = items.getArrowRopeLength(consumable);
        NamespacedKey key = items.getArrowRopeLengthKey();

        arrow.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, ropeLength);
    }

    // ==================== ROPE ARROW - LANDING ====================

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        NamespacedKey key = items.getArrowRopeLengthKey();
        if (!arrow.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) return;

        int ropeLength = arrow.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

        // Arrow hit an entity - drop rope coil
        if (event.getHitEntity() != null) {
            Location dropLoc = arrow.getLocation();
            arrow.getWorld().dropItemNaturally(dropLoc, items.createRopeCoil(ropeLength));
            arrow.remove();
            return;
        }

        // Arrow hit a block
        Block hitBlock = event.getHitBlock();
        if (hitBlock == null) {
            arrow.remove();
            return;
        }

        BlockFace hitFace = event.getHitBlockFace();
        Location placementLoc;

        if (hitFace == BlockFace.DOWN) {
            // Arrow hit underside of block - place rope hanging below that block
            placementLoc = hitBlock.getRelative(BlockFace.DOWN).getLocation();
        } else if (hitFace == BlockFace.UP) {
            // Arrow hit top of block - can't hang rope from above
            // Check if there's a block above we can hang from
            Block above = hitBlock.getRelative(BlockFace.UP, 2);
            if (above.getType().isSolid()) {
                placementLoc = hitBlock.getRelative(BlockFace.UP).getLocation();
            } else {
                // No block above - check if we can place fence and hang rope
                Block fenceBlock = hitBlock.getRelative(BlockFace.UP);
                Block belowFence = fenceBlock.getRelative(BlockFace.DOWN);

                // If block below fence is solid, can't hang rope - drop coil
                if (belowFence.getType().isSolid()) {
                    arrow.getWorld().dropItemNaturally(arrow.getLocation(), items.createRopeCoil(ropeLength));
                    arrow.remove();
                    return;
                }

                fenceBlock.setType(config.getFenceMaterial());
                fenceBlock.getState().update(true, true); // Update for fence connections

                // Rope hangs from fence, so length is reduced by 1
                if (ropeLength > 1) {
                    ropes.placeRope(fenceBlock.getLocation(), ropeLength - 1);
                }
                arrow.remove();
                return;
            }
        } else {
            // Arrow hit side of block - place rope at the hit location
            Block adjacentBlock = hitBlock.getRelative(hitFace);

            // Check for block above to hang from
            Block above = adjacentBlock.getRelative(BlockFace.UP);
            if (above.getType().isSolid()) {
                placementLoc = adjacentBlock.getLocation();
            } else {
                // No anchor - check if we can place fence
                Block belowAdjacent = adjacentBlock.getRelative(BlockFace.DOWN);

                // If block below is solid, can't hang rope - drop coil
                if (belowAdjacent.getType().isSolid()) {
                    arrow.getWorld().dropItemNaturally(arrow.getLocation(), items.createRopeCoil(ropeLength));
                    arrow.remove();
                    return;
                }

                adjacentBlock.setType(config.getFenceMaterial());
                adjacentBlock.getState().update(true, true); // Update for fence connections

                if (ropeLength > 1) {
                    ropes.placeRope(belowAdjacent.getLocation(), ropeLength - 1);
                }
                arrow.remove();
                return;
            }
        }

        // Place the rope
        ropes.placeRope(placementLoc, ropeLength);
        arrow.remove();
    }
}
