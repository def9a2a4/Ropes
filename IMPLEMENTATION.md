# Ropes Plugin Implementation Plan

## Overview
A Paper 1.21 plugin implementing rope mechanics: Rope Coil items, climbable Rope Blocks, and Rope Arrows.

## Current Status

| File | Status |
|------|--------|
| `RopesPlugin.java` | ✅ Complete |
| `Config.java` | ✅ Complete |
| `Items.java` | ✅ Complete |
| `Display.java` | ✅ Complete |
| `Ropes.java` | ✅ Complete |
| `config.yml` | ✅ Complete |
| `Listeners.java` | ❌ **Not implemented** |

**Minor TODO:** bStats plugin ID is placeholder `12345` in RopesPlugin.java

---

## Remaining Work: Listeners.java

Create `src/main/java/anon/def9a2a4/ropes/Listeners.java` implementing:

### 1. Crafting Events
- **PrepareItemCraftEvent**: Set correct output length for rope coil combining and rope arrow crafting

### 2. Block Interactions
- **PlayerInteractEvent (LEFT_CLICK_BLOCK on rope)**: Break rope, drop coil(s)
- **PlayerInteractEvent (RIGHT_CLICK_BLOCK on rope with coil)**: Extend rope
- **PlayerInteractEvent (RIGHT_CLICK_BLOCK on solid block with coil)**: Place new rope below
- **BlockBreakEvent**: If block above rope anchor breaks → break entire rope
- **BlockExplodeEvent/EntityExplodeEvent**: Handle explosion damage to ropes
- **BlockPistonExtendEvent/BlockPistonRetractEvent**: Break rope if piston affects it

### 3. Climbing Mechanics
- **PlayerMoveEvent**: Detect players adjacent to rope blocks
  - If jumping (velocity Y > 0): Apply upward velocity (climb speed from config)
  - If sneaking: Apply downward velocity
  - Otherwise: Cancel vertical velocity to hold position

### 4. Arrow Events
- **EntityShootBowEvent**: If shooting rope arrow, tag the Arrow entity with rope length PDC
- **ProjectileHitEvent (block)**:
  - If block above exists: place rope of stored length
  - If no block above: place fence block + (length-1) rope hanging down
  - Remove arrow entity
- **ProjectileHitEvent (entity)**: Drop rope arrow as item, remove arrow entity

---

## Architecture

```
src/main/java/anon/def9a2a4/ropes/
├── RopesPlugin.java    ✅ Main plugin class
├── Config.java         ✅ Configuration handler
├── Items.java          ✅ Rope coil & rope arrow item creation
├── Ropes.java          ✅ Core rope placement/breaking logic
├── Display.java        ✅ Display entity creation/removal
└── Listeners.java      ❌ All event listeners (TO BE CREATED)
```

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
  place-material: OAK_FENCE
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

| File | Status |
|------|--------|
| `src/main/java/anon/def9a2a4/ropes/RopesPlugin.java` | ✅ Done |
| `src/main/java/anon/def9a2a4/ropes/Config.java` | ✅ Done |
| `src/main/java/anon/def9a2a4/ropes/Items.java` | ✅ Done |
| `src/main/java/anon/def9a2a4/ropes/Display.java` | ✅ Done |
| `src/main/java/anon/def9a2a4/ropes/Ropes.java` | ✅ Done |
| `src/main/java/anon/def9a2a4/ropes/Listeners.java` | ❌ TODO |
| `src/main/resources/config.yml` | ✅ Done |
