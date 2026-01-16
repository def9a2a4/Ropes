we want this plugin to be the "Ropes" plugin. it will implement:

- a craftable "Rope Coil" item, which is a retextured player head
  - crafted with 6 string, unshaped
  - has lore "2 meters of rope"
  - can be combined, unshaped, with other rope coils, to make longer ropes. data stored in lore. up to 16 meters of rope per coil
  - can be placed, as long as there is a block above it
  - creates N meters of rope blocks hanging down

- a "Rope Block" is a "waxed exposed copper chain", forced into the vertical position, with a display entity of the same player head on it
  - display entity is stretched vertically by 2x and horizontally scaled by 0.2x in each direction
  - standing next to a rope block and pressing jump will move the player up, pressing sneak will move the player down. otherwise the player does not move vertically, but is allowed to move horizontally
  - left-clicking (break) a rope block will instantly break it and drop a rope coil with the appropriate length. it should remove the display entities and the chain blocks
  - right clicking a rope block with a rope coil will extend the rope by the length of the coil
  - if the block above a rope block is broken, the entire rope breaks and drops a rope coil of the appropriate length

- a "Rope Arrow" is an arrow that, when it lands on a block, places a rope block hanging down from that block
  - crafting recipe is `- A - | - S - | S R S` where A is arrow, S is stick, R is rope coil
  - if the arrow lands in a block where there is a block above it, it will place a rope block with length equal to the length of the rope coil used in crafting
  - if there is no block above where the arrow lands, the arrow places an oak fence block where it landed, and ropes equal to the length of the coil minus 1 hanging down from that fence block
  - if the arrow hits an entity, it drops itself as an item
  - the rope arrow item has lore "Shoots a rope where it lands", and has an enchanted glint
  - the arrow entity itself always dissapears on landing, regardless of whether it places a rope or drops as an item


Some design guidelines:
- make things configurable where reasonable. crafting recipes, climb speed, head textures, max lengths, etc
- think about edge cases
  - in particular, handle the ropes breaking from non-player causes (explosions, pistons, etc) gracefully. 
  - when the rope is too long for a single coil, drop multiple coils
- detection of rope blocks (for climbing and breaking) is done by searching for display entities with appropriate tags
- keep the code as simple and clean as possible

