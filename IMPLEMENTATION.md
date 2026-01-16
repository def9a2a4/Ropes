# Ropes Plugin Implementation Plan

## Overview
A Paper 1.21 plugin implementing rope mechanics: Rope Coil items, climbable Rope Blocks, and Rope Arrows.

## Architecture

```
src/main/java/anon/def9a2a4/ropes/
├── RopesPlugin.java    # Main plugin class
├── Config.java         # Configuration handler
├── Items.java          # Rope coil & rope arrow item creation
├── Ropes.java          # Core rope placement/breaking logic + RopeData record
├── Display.java        # Display entity creation/removal
└── Listeners.java      # All event listeners (crafting, climbing, arrows, blocks)
```

---

## Parallel Implementation Tracks

### Track 1: Core Infrastructure & Rope Coil Item
**No dependencies on other tracks**

1. **RopesPlugin.java** - Main plugin class
   - onEnable/onDisable lifecycle
   - Register Listeners
   - Load configuration
   - Register crafting recipes

2. **Config.java** - Configuration
   - Head texture URL/value
   - Climb speed (blocks per tick)
   - Max rope length per coil (default 16)
   - Default rope length (default 2)
   - Crafting recipe toggles
   - Material overrides (chain type, fence type)

3. **config.yml** - Default configuration file

4. **Items.java** - All custom items
   - `createRopeCoil(int meters)` - Create player head with texture, lore, PDC data
   - `getRopeLength(ItemStack)` - Extract length from PDC
   - `isRopeCoil(ItemStack)` - Validation check
   - `combineCoils(ItemStack, ItemStack)` - Merge lengths (cap at max)
   - `createRopeArrow(int ropeLength)` - Arrow with glint, lore, PDC data
   - `isRopeArrow(ItemStack)` - Validation
   - `getArrowRopeLength(ItemStack)` - Extract length from PDC

---

### Track 2: Rope Block System (Core Mechanics)
**Depends on: Track 1 (Items, Config)**

1. **Display.java** - Display entity management
   - `spawnRopeDisplay(Location)` - Create stretched player head display
   - `removeRopeDisplay(Location)` - Remove display entity at location
   - `findRopeDisplaysInColumn(Location)` - Find all rope displays in a vertical column
   - `isRopeDisplay(Entity)` - Check entity tags
   - Tag format: `ropes_display`

2. **Ropes.java** - Core rope logic + data
   - `record RopeData(Location anchor, int length)` - Rope metadata
   - `placeRope(Location anchor, int length)` - Place chain blocks + display entities downward
   - `breakRope(Location anyRopeBlock)` - Remove entire rope, return total length
   - `extendRope(Location bottomBlock, int additionalLength)` - Add length to existing rope
   - `findRopeAnchor(Location ropeBlock)` - Trace up to find anchor
   - `getRopeLength(Location anchor)` - Count rope blocks from anchor
   - `isRopeBlock(Location)` - Check for chain + display entity
   - Handle air gaps, obstacles when placing

---

### Track 3: Listeners
**Depends on: Track 1 (Items, Config), Track 2 (Ropes, Display)**

1. **Listeners.java** - All event handlers in one class

   **Crafting:**
   - PrepareItemCraftEvent: Set correct output for rope coil combining and rope arrow crafting

   **Block Interactions:**
   - PlayerInteractEvent (LEFT_CLICK_BLOCK): Break rope, drop coil(s)
   - PlayerInteractEvent (RIGHT_CLICK_BLOCK on rope): Extend rope with held coil
   - PlayerInteractEvent (RIGHT_CLICK_BLOCK with coil): Place new rope
   - BlockBreakEvent: Detect if block above rope anchor breaks → break entire rope
   - BlockExplodeEvent/EntityExplodeEvent: Handle explosion damage to ropes
   - BlockPistonExtendEvent/BlockPistonRetractEvent: Handle piston interactions

   **Climbing:**
   - PlayerMoveEvent: Detect players near rope blocks
   - Apply upward velocity on jump, downward on sneak
   - Cancel vertical velocity otherwise

   **Arrow:**
   - EntityShootBowEvent: Tag arrow entity with rope data when shot
   - ProjectileHitEvent (block): Place rope or fence+rope, remove arrow
   - ProjectileHitEvent (entity): Drop arrow as item, remove entity

---

## Implementation Order

```
Parallel Start:
├── Agent A: Track 1 (Config, Items, Plugin) ─────────────────►
├── Agent B: Track 2 (Display, Ropes) [waits for Track 1]─────►
└── Agent C: Track 3 (Listeners) [waits for Track 1 & 2]──────►
```

**Recommended parallel assignment:**

| Agent | Track | Start Condition |
|-------|-------|-----------------|
| A | Track 1: Core Infrastructure | Immediate |
| B | Track 2: Rope Block System | After Config + Items stubs exist |
| C | Track 3: Listeners | After Ropes.isRopeBlock exists |

---

## Key Technical Details

### Player Head Texture
```java
// Using base64 texture value for custom head
// Texture: eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGY1MTM2MWE4MGM3MmQ2ODUwN2E0YzVkY2I3ZDY3MWNmMGZmZGMzNTc3YmNlOWU3OWVmOWFmOTMxNTM2YmE1MyJ9fX0=
ItemStack head = new ItemStack(Material.PLAYER_HEAD);
SkullMeta meta = (SkullMeta) head.getItemMeta();
PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
profile.setProperty(new ProfileProperty("textures", BASE64_TEXTURE_VALUE));
meta.setPlayerProfile(profile);
```

### Persistent Data Container (PDC) for Length Storage
```java
NamespacedKey ROPE_LENGTH_KEY = new NamespacedKey(plugin, "rope_length");
meta.getPersistentDataContainer().set(ROPE_LENGTH_KEY, PersistentDataType.INTEGER, length);
```

### Display Entity for Rope Visual
```java
ItemDisplay display = world.spawn(location, ItemDisplay.class, entity -> {
    entity.setItemStack(ropeHeadItem);
    entity.setTransformation(new Transformation(
        new Vector3f(0, 0, 0),           // translation
        new AxisAngle4f(0, 0, 0, 1),     // left rotation
        new Vector3f(0.2f, 2f, 0.2f),    // scale (0.2x, 2x, 0.2x)
        new AxisAngle4f(0, 0, 0, 1)      // right rotation
    ));
    entity.addScoreboardTag("ropes_display");
});
```

### Climbing Detection (Velocity-Based)
- Use PlayerMoveEvent to detect when player is adjacent to rope block
- Check player's current inputs:
  - If player is jumping (velocity Y > 0): Apply upward velocity
  - If player is sneaking (player.isSneaking()): Apply downward velocity
  - Otherwise: Cancel vertical velocity to keep player in place
- Velocity approach feels more natural than teleportation

### Handling Rope Drops (Length > 16)
```java
int remaining = totalLength;
while (remaining > 0) {
    int coilLength = Math.min(remaining, config.getMaxCoilLength());
    world.dropItemNaturally(location, RopeCoilItem.create(coilLength));
    remaining -= coilLength;
}
```

---

## Edge Cases

1. **Rope placement blocked by existing blocks** - Place as much rope as fits, refund unused length as a smaller coil dropped to player
2. **Rope extends into unloaded chunk** - Only place in loaded chunks, refund unused length
3. **Multiple players breaking same rope** - Use synchronization or first-come handling
4. **Piston pushes rope block** - Break entire rope on piston event
5. **Explosion destroys middle of rope** - Detect via BlockExplodeEvent, break entire rope
6. **Arrow lands on non-solid block** - Check for solid block, handle gracefully
7. **Arrow shot into void** - Just remove arrow, no placement
8. **Rope coil combination overflow** - Cap at max, warn player or drop remainder

---

## Configuration (config.yml)

```yaml
# Rope Coil Settings
rope-coil:
  default-length: 2
  max-length: 16
  head-texture: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGY1MTM2MWE4MGM3MmQ2ODUwN2E0YzVkY2I3ZDY3MWNmMGZmZGMzNTc3YmNlOWU3OWVmOWFmOTMxNTM2YmE1MyJ9fX0="

# Rope Block Settings
rope-block:
  chain-material: WAXED_EXPOSED_COPPER_CHAIN
  climb-speed: 0.2  # blocks per tick when climbing

# Rope Arrow Settings
rope-arrow:
  fence-material: OAK_FENCE
  glint: true

# Crafting Recipes
recipes:
  rope-coil-enabled: true
  rope-coil-combine-enabled: true
  rope-arrow-enabled: true
```

---

## Verification Plan

1. **Unit Tests** (if time permits)
   - RopeCoilItem creation and length extraction
   - RopeManager placement/breaking logic

2. **Manual Testing Checklist**
   - [ ] Craft rope coil with 6 string
   - [ ] Combine two rope coils, verify length addition
   - [ ] Place rope coil under solid block, verify chain + display entities
   - [ ] Climb rope with jump, descend with sneak
   - [ ] Break rope block, verify coil drop with correct length
   - [ ] Right-click rope with coil to extend
   - [ ] Break block above rope, verify entire rope breaks
   - [ ] Craft rope arrow
   - [ ] Shoot rope arrow at block with ceiling, verify rope placement
   - [ ] Shoot rope arrow at block without ceiling, verify fence + rope
   - [ ] Shoot rope arrow at entity, verify item drop
   - [ ] Test explosion near rope
   - [ ] Test piston pushing rope block

---

## Files to Create

| File | Track |
|------|-------|
| `src/main/java/anon/def9a2a4/ropes/RopesPlugin.java` | 1 |
| `src/main/java/anon/def9a2a4/ropes/Config.java` | 1 |
| `src/main/java/anon/def9a2a4/ropes/Items.java` | 1 |
| `src/main/java/anon/def9a2a4/ropes/Display.java` | 2 |
| `src/main/java/anon/def9a2a4/ropes/Ropes.java` | 2 |
| `src/main/java/anon/def9a2a4/ropes/Listeners.java` | 3 |
| `src/main/resources/config.yml` | 1 |
