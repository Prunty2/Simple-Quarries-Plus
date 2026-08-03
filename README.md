# ⛏️ Simple Quarries Plus 

A feature-rich Fabric quarry mod for Minecraft 26.2 that automates vertical mining using fuel and pickaxes. Mine entire shafts automatically with configurable area sizes and full Fortune/Silk Touch support.

## ✨ Features

### Core Functionality
- **Automated Mining**: Place a quarry and watch it excavate a vertical shaft straight down
- **Tool-Based Mining**: Insert any pickaxe - the quarry uses its mining speed, enchantments, and durability
- **Fuel System**: Power your quarry with any standard furnace fuel (coal, lava buckets, wood, etc.)
- **Smart Block Selection**: Automatically skips air, bedrock, and other quarries
- **Large Inventory**: 24-slot output storage with automatic item collection
- **Enchantment Support**: Full compatibility with Fortune, Silk Touch, Efficiency, and Unbreaking

### Upgrade System
- **Expandable Mining Area**: Start with 5×5, upgrade to a maximum of 15×15
- **Quarry Upgrade Templates**: Found in dungeon chests, bastions, end cities, and more
- **Persistent Upgrades**: Upgraded quarries keep their level when broken and replaced
- **Simple Upgrading**: Craft quarry + upgrade template to add +2 blocks to mining width

### Automation Support
- **Hopper Compatible**: Extract items from bottom, insert fuel from top, insert pickaxes from sides
- **Comparator Support**: Outputs redstone signal based on inventory fullness
- **Item Overflow Protection**: Extra items drop above the quarry if inventory is full

## 📖 How to Use

### Basic Setup
1. Craft a Quarry block and place it where you want to mine
2. Right-click to open the quarry interface
3. Insert a pickaxe in the top-left slot
4. Add fuel to the slot below the pickaxe
5. The quarry will begin mining and storing blocks in the output grid

### Quarry Crafting

Arrange the ingredients in a crafting table as follows:

```text
Redstone       Redstone        Redstone
Iron Block     Diamond Pickaxe Iron Block
Cobblestone    Cobblestone     Cobblestone
```

### Mining Mechanics
- The quarry mines in a **square area** centered on itself, going straight down
- **Mining speed** depends on pickaxe type:
  - Golden pickaxes are fastest
  - Netherite and diamond are very efficient
  - Efficiency enchantment significantly increases speed
- **Durability** is consumed per block mined (reduced by Unbreaking)
- **Fuel** is consumed per block mined - different fuels mine different amounts of blocks

### ⚡ Fuel Efficiency
Here's how many blocks different fuels can mine:
- **Lava Bucket**: 100 blocks
- **Coal Block**: 80 blocks
- **Dried Kelp Block**: 20 blocks
- **Blaze Rod**: 12 blocks
- **Coal/Charcoal**: 8 blocks each
- **Wood/Planks**: 2 blocks per item
- **Sticks/Bamboo**: 1 block each

### 📦 Upgrading Your Quarry
1. Find a **Quarry Upgrade Template** in loot chests:
   - Bastions (30% chance - best source)
   - Stronghold libraries and corridors (20% chance)
   - Abandoned mineshafts (10% chance)
   - Nether fortresses (10% chance)
   - End cities (8% chance)
   - Desert pyramids (5% chance)
   - Dungeons and ruined portals (3% chance)

2. Place your Quarry block and one Upgrade Template in a crafting grid (shapeless)
3. Each upgrade increases mining width by 2 blocks

**Upgrade Progression**:
- **Level 0**: 5×5 (25 blocks per layer)
- **Level 1**: 7×7 (49 blocks per layer)
- **Level 2**: 9×9 (81 blocks per layer)
- **Level 3**: 11×11 (121 blocks per layer)
- **Level 4**: 13×13 (169 blocks per layer)
- **Level 5 (Max)**: 15×15 (225 blocks per layer)

### Automation Tips
- **Input fuel automatically**: Place a hopper on top pointing into the quarry
- **Extract items automatically**: Place a hopper underneath or use item pipes/ducts
- **Insert pickaxes**: Use hoppers on the sides for automatic pickaxe feeding
- **Monitor fullness**: Connect a comparator to detect when output inventory is full

### 💡 Pro Tips
- Use **Unbreaking III** pickaxes to significantly reduce pickaxe consumption
- **Fortune III** works perfectly - get extra diamonds, coal, redstone, etc.
- **Silk Touch** is supported - collect grass blocks, stone, ores as-is
- **Efficiency V** dramatically increases mining speed
- Keep the quarry **chunk-loaded** for continuous operation while you're away
- Upgrade to 15×15 for maximum efficiency - it mines 9x more blocks per layer than the base quarry
- Clear lava pools above the quarry area if you don't want fluids in your shaft

