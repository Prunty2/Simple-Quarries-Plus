package com.simplequarries.block.entity;

import com.simplequarries.QuarryUpgrades;
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
import net.minecraft.inventory.SidedInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
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
 * - Slots 2-25: Output slots (24 slots = 4 rows x 6 cols)
 * - Slots 26-34: Filter slots (9 slots = 3x3 grid)
 */
public class QuarryBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<QuarryScreenHandler.QuarryScreenData>, Inventory, SidedInventory {
    
    // Inventory slot indices
    public static final int PICKAXE_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_START = 2;
    public static final int OUTPUT_SLOTS = 24;  // 4 rows x 6 cols
    public static final int FILTER_START = 26;
    public static final int FILTER_SLOTS = 9;   // 3x3 grid
    public static final int INVENTORY_SIZE = FILTER_START + FILTER_SLOTS; // 35 total slots

    // Filter modes
    public static final int FILTER_DISABLED = 0;
    public static final int FILTER_WHITELIST = 1;
    public static final int FILTER_BLACKLIST = 2;

    // Sided inventory slot access arrays
    private static final int[] TOP_SLOTS = { FUEL_SLOT };
    private static final int[] BOTTOM_SLOTS = createBottomSlots();
    private static final int[] SIDE_SLOTS = { PICKAXE_SLOT };

    // Valid pickaxes that can be used
    private static final Set<Item> VALID_PICKAXES = Set.of(
            Items.WOODEN_PICKAXE,
            Items.STONE_PICKAXE,
            Items.COPPER_PICKAXE,
            Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE,
            Items.DIAMOND_PICKAXE,
            Items.NETHERITE_PICKAXE
    );

    // Fuel burn times mapped to blocks mined
    private static final Map<Item, Integer> FUEL_VALUES = Map.ofEntries(
            Map.entry(Items.COAL, 8),
            Map.entry(Items.CHARCOAL, 8),
            Map.entry(Items.BLAZE_ROD, 12),
            Map.entry(Items.DRIED_KELP_BLOCK, 20),
            Map.entry(Items.COAL_BLOCK, 80),
            Map.entry(Items.LAVA_BUCKET, 100),
            Map.entry(Items.STICK, 1),
            Map.entry(Items.BAMBOO, 1),
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

    // Property delegate for syncing data to the screen (6 properties now)
    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> lastFuelTime;
                case 2 -> miningProgress;
                case 3 -> ticksPerBlock;
                case 4 -> filterMode;
                case 5 -> 1; // chunk loading always enabled
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
                case 4 -> filterMode = MathHelper.clamp(value, 0, 2);
                case 5 -> {} // chunk loading always enabled, ignore
            }
        }

        @Override
        public int size() {
            return 6;
        }
    };

    // State tracking
    private int burnTime = 0;
    private int lastFuelTime = 0;
    private int miningProgress = 0;
    private int ticksPerBlock = 0;
    private int currentDepth = 1;
    private int areaIndex = 0;
    private int upgradeCount = 0;
    private int speedUpgradeCount = 0;
    private int filterMode = FILTER_DISABLED;
    private boolean wasChunkForced = false;  // Track if we forced the chunk

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

        ServerWorld serverWorld = (ServerWorld) world;
        boolean dirty = false;
        ItemStack pickaxe = quarry.getStack(PICKAXE_SLOT);

        // Redstone control: if powered, pause mining
        if (world.isReceivingRedstonePower(pos)) {
            quarry.resetProgress();
            quarry.updateChunkLoading(serverWorld, false);
            return;
        }

        // Check if we have a valid pickaxe
        if (!quarry.isValidPickaxe(pickaxe)) {
            quarry.resetProgress();
            quarry.ticksPerBlock = 0;
            quarry.updateChunkLoading(serverWorld, false);
            return;
        }

        // Update mining speed based on pickaxe tier + speed upgrades
        quarry.ticksPerBlock = quarry.getTicksPerBlockFor(pickaxe);

        // Check fuel - consume new fuel if needed
        if (quarry.burnTime <= 0) {
            if (!quarry.tryConsumeFuel()) {
                quarry.resetProgress();
                quarry.updateChunkLoading(serverWorld, false);
                return;
            }
            dirty = true;
        }

        // Safety check
        if (quarry.ticksPerBlock <= 0) {
            quarry.resetProgress();
            quarry.updateChunkLoading(serverWorld, false);
            return;
        }

        // Quarry is actively mining - update chunk loading
        quarry.updateChunkLoading(serverWorld, true);

        // Increment mining progress
        quarry.miningProgress++;

        // Check if we've completed mining a block
        if (quarry.miningProgress >= quarry.ticksPerBlock) {
            quarry.miningProgress = 0;

            // Find and mine the next block
            BlockPos target = quarry.findNextTarget(serverWorld);
            if (target != null) {
                if (quarry.breakBlock(serverWorld, target, pickaxe)) {
                    quarry.burnTime = Math.max(0, quarry.burnTime - 1);
                    dirty = true;
                }
            } else {
                // Quarry has finished mining its entire area
                quarry.resetProgress();
                quarry.updateChunkLoading(serverWorld, false);
                // Play level-up sound so the player knows
                world.playSound(null, pos, net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP,
                    net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
                return;
            }
        }

        if (dirty) {
            quarry.markDirty();
        }
    }

    // ==================== Chunk Loading ====================

    /**
     * Update chunk loading state based on whether the quarry is actively mining
     */
    private void updateChunkLoading(ServerWorld world, boolean shouldBeActive) {
        boolean shouldForce = shouldBeActive; // always chunk load when active
        if (shouldForce != wasChunkForced) {
            ChunkPos chunkPos = new ChunkPos(pos);
            world.setChunkForced(chunkPos.x, chunkPos.z, shouldForce);
            wasChunkForced = shouldForce;
        }
    }

    /**
     * Called when the quarry is removed - ensure chunk is unforced
     */
    public void onRemoved(ServerWorld world) {
        if (wasChunkForced) {
            ChunkPos chunkPos = new ChunkPos(pos);
            world.setChunkForced(chunkPos.x, chunkPos.z, false);
            wasChunkForced = false;
        }
    }

    // Chunk loading is always enabled when the quarry is actively mining

    // ==================== Filter System ====================

    public int getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(int mode) {
        this.filterMode = MathHelper.clamp(mode, 0, 2);
        markDirty();
    }

    public void cycleFilterMode() {
        setFilterMode((filterMode + 1) % 3);
    }

    /**
     * Check if a specific drop item should be kept based on filter settings.
     * Filters match against the actual DROP ITEMS, not the block being mined.
     * This way putting cobblestone in the filter works even though the block is stone.
     * - Whitelist: only keep drops that match the filter
     * - Blacklist: void drops that match the filter
     */
    private boolean shouldKeepDrop(ItemStack drop) {
        if (filterMode == FILTER_DISABLED) {
            return true;
        }

        boolean matchesFilter = false;
        for (int i = FILTER_START; i < FILTER_START + FILTER_SLOTS; i++) {
            ItemStack filterStack = items.get(i);
            if (!filterStack.isEmpty() && filterStack.getItem() == drop.getItem()) {
                matchesFilter = true;
                break;
            }
        }

        if (filterMode == FILTER_WHITELIST) {
            return matchesFilter; // Only keep matching drops
        } else { // FILTER_BLACKLIST
            return !matchesFilter; // Void matching drops
        }
    }

    // ==================== Mining Logic ====================

    private void resetProgress() {
        miningProgress = 0;
    }

    private boolean tryConsumeFuel() {
        ItemStack fuel = getStack(FUEL_SLOT);
        int gainedBlocks = getFuelValue(fuel);

        if (gainedBlocks <= 0) {
            lastFuelTime = 0;
            return false;
        }

        Item fuelItem = fuel.getItem();
        fuel.decrement(1);

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

    public int getFuelValue(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        }
        Integer value = FUEL_VALUES.get(fuel.getItem());
        return value != null ? value : 0;
    }

    /**
     * Break a block and collect its drops
     */
    private boolean breakBlock(ServerWorld world, BlockPos target, ItemStack pickaxe) {
        BlockState targetState = world.getBlockState(target);
        
        if (targetState.isAir() || targetState.getHardness(world, target) < 0) {
            return false;
        }

        // Get the drops using the pickaxe (Fortune and Silk Touch are handled automatically
        // by getDroppedStacks since the pickaxe's enchantments affect the loot context)
        List<ItemStack> drops = Block.getDroppedStacks(targetState, world, target, world.getBlockEntity(target), null, pickaxe);
        
        boolean removed = world.breakBlock(target, false);

        if (!removed) {
            return false;
        }

        // Insert drops into output inventory, filtering per-item based on filter settings
        for (ItemStack drop : drops) {
            if (!shouldKeepDrop(drop)) {
                continue; // Void this drop
            }
            ItemStack remainder = insertIntoOutputs(drop.copy());
            if (!remainder.isEmpty()) {
                Block.dropStack(world, pos.up(), remainder);
            }
        }

        damagePickaxe(pickaxe);
        return true;
    }

    private void damagePickaxe(ItemStack pickaxe) {
        if (world instanceof ServerWorld serverWorld && pickaxe.isDamageable()) {
            int unbreaking = getEnchantmentLevel(net.minecraft.enchantment.Enchantments.UNBREAKING, pickaxe);
            boolean damage = true;
            if (unbreaking > 0) {
                if (serverWorld.getRandom().nextInt(unbreaking + 1) != 0) {
                    damage = false;
                }
            }
            if (damage) {
                int currentDamage = pickaxe.getDamage();
                int maxDamage = pickaxe.getMaxDamage();
                if (currentDamage + 1 >= maxDamage) {
                    setStack(PICKAXE_SLOT, ItemStack.EMPTY);
                } else {
                    pickaxe.setDamage(currentDamage + 1);
                    setStack(PICKAXE_SLOT, pickaxe);
                }
            }
        }
    }

    /**
     * Find the next block to mine, respecting filters
     */
    @Nullable
    private BlockPos findNextTarget(ServerWorld world) {
        int attempts = 0;
        int maxAttempts = Math.max(512, getTotalAreaSlots() * 2);

        while (pos.getY() - currentDepth >= world.getBottomY() && attempts < maxAttempts) {
            BlockPos offset = getOffsetForIndex(areaIndex);
            BlockPos target = pos.add(offset.getX(), -currentDepth, offset.getZ());
            advancePointer();
            attempts++;

            BlockState state = world.getBlockState(target);
            
            if (state.isAir()) {
                continue;
            }

            if (state.getHardness(world, target) < 0) {
                continue;
            }

            if (state.getBlock() == SimpleQuarries.QUARRY_BLOCK) {
                continue;
            }

            return target;
        }

        return null;
    }

    private void advancePointer() {
        areaIndex++;
        if (areaIndex >= getTotalAreaSlots()) {
            areaIndex = 0;
            currentDepth++;
        }
    }

    private ItemStack insertIntoOutputs(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SLOTS; i++) {
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

        for (int i = OUTPUT_START; i < OUTPUT_START + OUTPUT_SLOTS; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty()) {
                items.set(i, stack.copy());
                markDirty();
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    public boolean isValidPickaxe(ItemStack stack) {
        return VALID_PICKAXES.contains(stack.getItem());
    }

    public int getUpgradeCount() {
        return upgradeCount;
    }

    public void setUpgradeCount(int count) {
        upgradeCount = QuarryUpgrades.clampUpgradeCount(count);
        clampAreaIndex();
        markDirty();
    }

    public int getSpeedUpgradeCount() {
        return speedUpgradeCount;
    }

    public void setSpeedUpgradeCount(int count) {
        speedUpgradeCount = QuarryUpgrades.clampSpeedCount(count);
        markDirty();
    }

    private void clampAreaIndex() {
        int maxIndex = Math.max(0, getTotalAreaSlots() - 1);
        areaIndex = MathHelper.clamp(areaIndex, 0, maxIndex);
    }

    private int getEnchantmentLevel(RegistryKey<Enchantment> enchantmentKey, ItemStack stack) {
        ItemEnchantmentsComponent enchantments = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack);
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(enchantmentKey)) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    /**
     * Get the mining speed (ticks per block) for a pickaxe, with speed upgrades applied
     */
    private int getTicksPerBlockFor(ItemStack pickaxe) {
        if (!isValidPickaxe(pickaxe)) {
            return 0;
        }

        Item item = pickaxe.getItem();
        int baseTicks = 0;
        if (item == Items.WOODEN_PICKAXE) baseTicks = 200;
        else if (item == Items.STONE_PICKAXE) baseTicks = 160;
        else if (item == Items.COPPER_PICKAXE) baseTicks = 140;
        else if (item == Items.IRON_PICKAXE) baseTicks = 120;
        else if (item == Items.GOLDEN_PICKAXE) baseTicks = 20;
        else if (item == Items.DIAMOND_PICKAXE) baseTicks = 80;
        else if (item == Items.NETHERITE_PICKAXE) baseTicks = 40;
        else return 0;

        // Apply Efficiency enchantment
        int efficiency = getEnchantmentLevel(net.minecraft.enchantment.Enchantments.EFFICIENCY, pickaxe);
        if (efficiency > 0) {
            double speedMultiplier = 1.0 + 0.25 * (efficiency * efficiency + 1);
            baseTicks = (int) Math.round(baseTicks / speedMultiplier);
        }

        // Apply speed upgrades (each reduces time by 20% multiplicatively)
        baseTicks = (int) Math.round(baseTicks * QuarryUpgrades.speedMultiplierForCount(speedUpgradeCount));

        return Math.max(1, baseTicks);
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void writeData(WriteView data) {
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
        data.putInt("UpgradeCount", upgradeCount);
        data.putInt("SpeedUpgradeCount", speedUpgradeCount);
        data.putInt("FilterMode", filterMode);
        // chunkLoaderEnabled removed — always on
    }

    @Override
    protected void readData(ReadView data) {
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
        upgradeCount = QuarryUpgrades.clampUpgradeCount(data.getInt("UpgradeCount", 0));
        speedUpgradeCount = QuarryUpgrades.clampSpeedCount(data.getInt("SpeedUpgradeCount", 0));
        filterMode = MathHelper.clamp(data.getInt("FilterMode", 0), 0, 2);
        // chunkLoaderEnabled removed — always on
        clampAreaIndex();
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

    private int getMiningAreaSize() {
        return QuarryUpgrades.areaForCount(upgradeCount);
    }

    private int getTotalAreaSlots() {
        int size = getMiningAreaSize();
        return size * size;
    }

    private BlockPos getOffsetForIndex(int index) {
        int size = getMiningAreaSize();
        int radius = size / 2;
        int xIndex = index % size;
        int zIndex = index / size;
        return new BlockPos(xIndex - radius, 0, zIndex - radius);
    }

    private static int[] createBottomSlots() {
        int[] slots = new int[OUTPUT_SLOTS + 1];
        slots[0] = FUEL_SLOT;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            slots[i + 1] = OUTPUT_START + i;
        }
        return slots;
    }

    // ==================== SidedInventory Implementation ====================

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            return BOTTOM_SLOTS;
        } else if (side == Direction.UP) {
            return TOP_SLOTS;
        } else {
            return SIDE_SLOTS;
        }
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == PICKAXE_SLOT) {
            return isValidPickaxe(stack);
        }
        if (slot == FUEL_SLOT) {
            return getFuelValue(stack) > 0;
        }
        // Filter slots accept any item (for reference)
        if (slot >= FILTER_START && slot < FILTER_START + FILTER_SLOTS) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        if (slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUT_SLOTS) {
            return true;
        }
        if (slot == FUEL_SLOT && stack.isOf(Items.BUCKET)) {
            return true;
        }
        return false;
    }
}
