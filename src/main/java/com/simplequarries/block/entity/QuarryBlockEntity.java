package com.simplequarries.block.entity;

import com.simplequarries.SimpleQuarries;
import com.simplequarries.screen.QuarryScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Quarry Block Entity - handles the quarry mining logic
 * 
 * Inventory layout:
 * - Slot 0: Pickaxe slot
 * - Slot 1: Fuel slot
 * - Slots 2-37: Output slots (36 slots = 4 rows x 9 cols)
 */
public class QuarryBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<QuarryScreenHandler.QuarryScreenData>, Inventory {
    
    // Inventory slot indices
    public static final int PICKAXE_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_SLOTS = 36;
    public static final int INVENTORY_SIZE = OUTPUT_START + OUTPUT_SLOTS; // 38 total slots

    // Mining pattern: 5x5 area centered on the quarry
    private static final BlockPos[] MINING_OFFSETS = createMiningOffsets();

    // Valid pickaxes that can be used
    private static final Set<Item> VALID_PICKAXES = Set.of(
            Items.WOODEN_PICKAXE,
            Items.STONE_PICKAXE,
            Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE
    );

    // Fuel burn times in ticks (like furnace) mapped to blocks mined
    // Furnace burns coal for 1600 ticks (8 items), so coal = 8 blocks
    private static final Map<Item, Integer> FUEL_VALUES = Map.ofEntries(
            Map.entry(Items.COAL, 8),
            Map.entry(Items.CHARCOAL, 8),
            Map.entry(Items.BLAZE_ROD, 12),
            Map.entry(Items.DRIED_KELP_BLOCK, 20),
            Map.entry(Items.COAL_BLOCK, 80),
            Map.entry(Items.LAVA_BUCKET, 100),
            Map.entry(Items.STICK, 1),
            Map.entry(Items.BAMBOO, 1),
            // Wooden items
            Map.entry(Items.OAK_LOG, 2),
            Map.entry(Items.SPRUCE_LOG, 2),
            Map.entry(Items.BIRCH_LOG, 2),
            Map.entry(Items.JUNGLE_LOG, 2),
            Map.entry(Items.ACACIA_LOG, 2),
            Map.entry(Items.DARK_OAK_LOG, 2),
            Map.entry(Items.MANGROVE_LOG, 2),
            Map.entry(Items.CHERRY_LOG, 2),
            Map.entry(Items.OAK_PLANKS, 2),
            Map.entry(Items.SPRUCE_PLANKS, 2),
            Map.entry(Items.BIRCH_PLANKS, 2),
            Map.entry(Items.JUNGLE_PLANKS, 2),
            Map.entry(Items.ACACIA_PLANKS, 2),
            Map.entry(Items.DARK_OAK_PLANKS, 2),
            Map.entry(Items.MANGROVE_PLANKS, 2),
            Map.entry(Items.CHERRY_PLANKS, 2),
            Map.entry(Items.BAMBOO_PLANKS, 2)
    );

    // Inventory storage
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    // Property delegate for syncing data to the screen
    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> lastFuelTime;
                case 2 -> miningProgress;
                case 3 -> ticksPerBlock;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> lastFuelTime = value;
                case 2 -> miningProgress = value;
                case 3 -> ticksPerBlock = value;
            }
        }

        @Override
        public int size() {
            return 4;
        }
    };

    // State tracking
    private int burnTime = 0;           // Remaining blocks that can be mined with current fuel
    private int lastFuelTime = 0;       // Last fuel item's total burn time (for progress bar)
    private int miningProgress = 0;     // Current progress towards mining next block
    private int ticksPerBlock = 0;      // Ticks needed to mine one block (based on pickaxe)
    private int currentDepth = 1;       // Current mining depth below quarry
    private int areaIndex = 0;          // Current position in MINING_OFFSETS array

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleQuarries.QUARRY_BLOCK_ENTITY, pos, state);
    }

    /**
     * Main tick function - called every game tick on the server
     */
    public static void tick(World world, BlockPos pos, BlockState state, QuarryBlockEntity quarry) {
        if (world.isClient()) {
            return;
        }

        boolean dirty = false;
        ItemStack pickaxe = quarry.getStack(PICKAXE_SLOT);

        // Check if we have a valid pickaxe
        if (!quarry.isValidPickaxe(pickaxe)) {
            quarry.resetProgress();
            quarry.ticksPerBlock = 0;
            return;
        }

        // Update mining speed based on pickaxe tier
        quarry.ticksPerBlock = quarry.getTicksPerBlockFor(pickaxe);

        // Check fuel - consume new fuel if needed
        if (quarry.burnTime <= 0) {
            if (!quarry.tryConsumeFuel()) {
                quarry.resetProgress();
                return;
            }
            dirty = true;
        }

        // Safety check
        if (quarry.ticksPerBlock <= 0) {
            quarry.resetProgress();
            return;
        }

        // Increment mining progress
        quarry.miningProgress++;

        // Check if we've completed mining a block
        if (quarry.miningProgress >= quarry.ticksPerBlock) {
            quarry.miningProgress = 0;

            // Find and mine the next block
            BlockPos target = quarry.findNextTarget((ServerWorld) world);
            if (target != null && quarry.breakBlock((ServerWorld) world, target, pickaxe)) {
                quarry.burnTime = Math.max(0, quarry.burnTime - 1);
                dirty = true;
            }
        }

        if (dirty) {
            quarry.markDirty();
        }
    }

    /**
     * Reset mining progress
     */
    private void resetProgress() {
        miningProgress = 0;
    }

    /**
     * Try to consume a fuel item from the fuel slot
     * @return true if fuel was consumed
     */
    private boolean tryConsumeFuel() {
        ItemStack fuel = getStack(FUEL_SLOT);
        int gainedBlocks = getFuelValue(fuel);

        if (gainedBlocks <= 0) {
            lastFuelTime = 0;
            return false;
        }

        Item fuelItem = fuel.getItem();
        fuel.decrement(1);

        // Handle items that leave a remainder (like lava bucket -> bucket)
        if (fuel.isEmpty()) {
            ItemStack remainder = fuelItem.getRecipeRemainder(fuel);
            if (!remainder.isEmpty()) {
                setStack(FUEL_SLOT, remainder.copy());
            }
        }

        burnTime += gainedBlocks;
        lastFuelTime = gainedBlocks;
        markDirty();
        return true;
    }

    /**
     * Get the fuel value (blocks that can be mined) for an item
     */
    public int getFuelValue(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        }

        // Check our predefined fuel values first
        Integer value = FUEL_VALUES.get(fuel.getItem());
        if (value != null) {
            return value;
        }

        // Fallback: Use a simple default for other items
        // In 1.21.10, the FuelRegistry API changed
        return 0;
    }

    /**
     * Break a block and collect its drops
     */
    private boolean breakBlock(ServerWorld world, BlockPos target, ItemStack pickaxe) {
        BlockState targetState = world.getBlockState(target);
        
        // Skip air and unbreakable blocks
        if (targetState.isAir() || targetState.getHardness(world, target) < 0) {
            return false;
        }

        // Get the drops using the pickaxe
        List<ItemStack> drops = Block.getDroppedStacks(targetState, world, target, world.getBlockEntity(target), null, pickaxe);
        
        // Break the block without dropping items (we handle drops manually)
        boolean removed = world.breakBlock(target, false);

        if (!removed) {
            return false;
        }

        // Insert drops into output inventory
        for (ItemStack drop : drops) {
            ItemStack remainder = insertIntoOutputs(drop.copy());
            // If we couldn't fit all items, drop them in the world
            if (!remainder.isEmpty()) {
                Block.dropStack(world, pos.up(), remainder);
            }
        }

        // Damage the pickaxe
        damagePickaxe(pickaxe);
        return true;
    }

    /**
     * Damage the pickaxe by 1 durability
     */
    private void damagePickaxe(ItemStack pickaxe) {
        if (world instanceof ServerWorld serverWorld && pickaxe.isDamageable()) {
            // Damage the pickaxe
            int currentDamage = pickaxe.getDamage();
            int maxDamage = pickaxe.getMaxDamage();
            
            if (currentDamage + 1 >= maxDamage) {
                // Pickaxe breaks
                setStack(PICKAXE_SLOT, ItemStack.EMPTY);
            } else {
                pickaxe.setDamage(currentDamage + 1);
                setStack(PICKAXE_SLOT, pickaxe);
            }
        }
    }

    /**
     * Find the next block to mine
     */
    @Nullable
    private BlockPos findNextTarget(ServerWorld world) {
        int attempts = 0;
        int maxAttempts = 512; // Prevent infinite loops

        while (pos.getY() - currentDepth >= world.getBottomY() && attempts < maxAttempts) {
            BlockPos offset = MINING_OFFSETS[areaIndex];
            BlockPos target = pos.add(offset.getX(), -currentDepth, offset.getZ());
            advancePointer();
            attempts++;

            BlockState state = world.getBlockState(target);
            
            // Skip air blocks
            if (state.isAir()) {
                continue;
            }

            // Skip unbreakable blocks (bedrock, etc.)
            if (state.getHardness(world, target) < 0) {
                continue;
            }

            // Don't mine other quarries
            if (state.getBlock() == SimpleQuarries.QUARRY_BLOCK) {
                continue;
            }

            return target;
        }

        return null;
    }

    /**
     * Advance the mining pointer to the next position
     */
    private void advancePointer() {
        areaIndex++;
        if (areaIndex >= MINING_OFFSETS.length) {
            areaIndex = 0;
            currentDepth++;
        }
    }

    /**
     * Insert an item stack into the output slots
     * @return remaining items that couldn't be inserted
     */
    private ItemStack insertIntoOutputs(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // First pass: try to merge with existing stacks
        for (int i = OUTPUT_START; i < INVENTORY_SIZE; i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.areItemsAndComponentsEqual(existing, stack)) {
                int transferable = Math.min(stack.getCount(), 
                        Math.min(getMaxCountPerStack(), existing.getMaxCount()) - existing.getCount());
                if (transferable > 0) {
                    existing.increment(transferable);
                    stack.decrement(transferable);
                    if (stack.isEmpty()) {
                        markDirty();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // Second pass: find empty slots
        for (int i = OUTPUT_START; i < INVENTORY_SIZE; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) {
                items.set(i, stack.copy());
                markDirty();
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    /**
     * Check if an item is a valid pickaxe
     */
    public boolean isValidPickaxe(ItemStack stack) {
        return VALID_PICKAXES.contains(stack.getItem());
    }

    /**
     * Get the mining speed (ticks per block) for a pickaxe
     */
    private int getTicksPerBlockFor(ItemStack pickaxe) {
        if (!isValidPickaxe(pickaxe)) {
            return 0;
        }

        Item item = pickaxe.getItem();
        
        // Mining speeds as specified:
        // Wooden: 10 seconds = 200 ticks
        // Stone: 8 seconds = 160 ticks
        // Iron: 6 seconds = 120 ticks
        // Gold: 1 second = 20 ticks
        // Diamond: 4 seconds = 80 ticks
        // Netherite: 2 seconds = 40 ticks
        
        if (item == Items.WOODEN_PICKAXE) return 200;
        if (item == Items.STONE_PICKAXE) return 160;
        if (item == Items.IRON_PICKAXE) return 120;
        if (item == Items.GOLDEN_PICKAXE) return 20;
        if (item == Items.DIAMOND_PICKAXE) return 80;
        if (item == Items.NETHERITE_PICKAXE) return 40;
        
        return 0;
    }

    /**
     * Get the property delegate for screen syncing
     */
    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void writeData(WriteView data) {
        // Save inventory using a sub-view for items
        WriteView.ListView itemsList = data.getList("Items");
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                WriteView slotData = itemsList.add();
                slotData.putByte("Slot", (byte) i);
                slotData.put("Item", ItemStack.CODEC, stack);
            }
        }
        
        data.putInt("BurnTime", burnTime);
        data.putInt("LastFuelTime", lastFuelTime);
        data.putInt("MiningProgress", miningProgress);
        data.putInt("TicksPerBlock", ticksPerBlock);
        data.putInt("Depth", currentDepth);
        data.putInt("AreaIndex", areaIndex);
    }

    @Override
    protected void readData(ReadView data) {
        // Load inventory
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        
        ReadView.ListReadView itemsList = data.getListReadView("Items");
        for (ReadView slotData : itemsList) {
            int slot = slotData.getByte("Slot", (byte) 0) & 255;
            if (slot < items.size()) {
                slotData.read("Item", ItemStack.CODEC).ifPresent(stack -> items.set(slot, stack));
            }
        }
        
        burnTime = data.getInt("BurnTime", 0);
        lastFuelTime = data.getInt("LastFuelTime", 0);
        miningProgress = data.getInt("MiningProgress", 0);
        ticksPerBlock = data.getInt("TicksPerBlock", 0);
        currentDepth = Math.max(1, data.getInt("Depth", 1));
        areaIndex = MathHelper.clamp(data.getInt("AreaIndex", 0), 0, MINING_OFFSETS.length - 1);
    }

    // ==================== Inventory Implementation ====================

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 64.0;
    }

    // ==================== Screen Handler Factory ====================

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new QuarryScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.simplequarries.quarry");
    }

    @Override
    public QuarryScreenHandler.QuarryScreenData getScreenOpeningData(ServerPlayerEntity player) {
        return new QuarryScreenHandler.QuarryScreenData(pos);
    }

    // ==================== Helper Methods ====================

    /**
     * Create the 5x5 mining pattern offsets
     */
    private static BlockPos[] createMiningOffsets() {
        BlockPos[] offsets = new BlockPos[25];
        int index = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                offsets[index++] = new BlockPos(x, 0, z);
            }
        }
        return offsets;
    }
}
