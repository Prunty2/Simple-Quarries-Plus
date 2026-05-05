package com.simplequarries.screen;

import com.simplequarries.SimpleQuarries;
import com.simplequarries.block.entity.QuarryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Quarry GUI
 */
public class QuarryScreenHandler extends AbstractContainerMenu {
    
    private final Container inventory;
    private final QuarryBlockEntity blockEntity;
    private final ContainerData propertyDelegate;

    /**
     * Data sent from server to client when opening the screen
     */
    public record QuarryScreenData(BlockPos pos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, QuarryScreenData> PACKET_CODEC = StreamCodec.ofMember(
                (data, buf) -> buf.writeBlockPos(data.pos),
                buf -> new QuarryScreenData(buf.readBlockPos())
        );
    }

    /**
     * Client-side constructor
     */
    public QuarryScreenHandler(int syncId, Inventory playerInventory, QuarryScreenData data) {
        this(syncId, playerInventory, getBlockEntity(playerInventory, data.pos()), new SimpleContainerData(6));
    }

    /**
     * Server-side constructor
     */
    public QuarryScreenHandler(int syncId, Inventory playerInventory, QuarryBlockEntity blockEntity, ContainerData propertyDelegate) {
        super(SimpleQuarries.QUARRY_SCREEN_HANDLER, syncId);
        
        this.blockEntity = blockEntity;
        this.inventory = blockEntity;
        this.propertyDelegate = propertyDelegate;

        checkContainerSize(inventory, QuarryBlockEntity.INVENTORY_SIZE);
        inventory.startOpen(playerInventory.player);

        // Slot 0: Pickaxe
        this.addSlot(new PickaxeSlot(blockEntity, QuarryBlockEntity.PICKAXE_SLOT, 10, 20));
        
        // Slot 1: Fuel
        this.addSlot(new FuelSlot(blockEntity, QuarryBlockEntity.FUEL_SLOT, 10, 58));

        // Slots 2-25: Output grid (4 rows x 6 cols)
        int outputStartX = 62;
        int outputStartY = 10;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 6; col++) {
                int slotIndex = QuarryBlockEntity.OUTPUT_START + row * 6 + col;
                int x = outputStartX + col * 18;
                int y = outputStartY + row * 18;
                this.addSlot(new OutputSlot(blockEntity, slotIndex, x, y));
            }
        }

        // Slots 26-34: Filter grid (3x3) - positioned in filter section
        int filterStartX = 100;
        int filterStartY = 96;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = QuarryBlockEntity.FILTER_START + row * 3 + col;
                int x = filterStartX + col * 18;
                int y = filterStartY + row * 18;
                this.addSlot(new FilterSlot(blockEntity, slotIndex, x, y));
            }
        }

        // Player inventory (3 rows x 9 cols)
        int playerInvY = 158;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        // Player hotbar
        int hotbarY = 220;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }

        addDataSlots(propertyDelegate);
    }

    private static QuarryBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
            return quarry;
        }
        throw new IllegalStateException("Quarry block entity not found at " + pos);
    }

    // Total quarry slots: pickaxe(1) + fuel(1) + output(24) + filter(9) = 35
    private static final int QUARRY_SLOT_COUNT = QuarryBlockEntity.INVENTORY_SIZE;

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();

            int playerSlotStart = QUARRY_SLOT_COUNT;
            int playerSlotEnd = playerSlotStart + 36;

            if (slotIndex < QUARRY_SLOT_COUNT) {
                // Moving from quarry to player inventory
                if (!this.moveItemStackTo(original, playerSlotStart, playerSlotEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to quarry
                if (blockEntity.isValidPickaxe(original)) {
                    if (!this.moveItemStackTo(original, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (blockEntity.getFuelValue(original) > 0) {
                    if (!this.moveItemStackTo(original, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
    }

    /**
     * Handle button clicks from the client (filter mode toggle, chunk loader toggle)
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Cycle filter mode: disabled -> whitelist -> blacklist -> disabled
            blockEntity.cycleFilterMode();
            return true;
        }
        return false;
    }

    // ==================== Property Getters ====================

    public int getBurnTime() {
        return propertyDelegate.get(0);
    }

    public int getLastFuelTime() {
        return propertyDelegate.get(1);
    }

    public int getMiningProgress() {
        return propertyDelegate.get(2);
    }

    public int getTicksPerBlock() {
        return propertyDelegate.get(3);
    }

    public int getFilterMode() {
        return propertyDelegate.get(4);
    }

    public boolean isChunkLoaderEnabled() {
        return propertyDelegate.get(5) != 0;
    }

    public int getScaledFuelProgress() {
        int burnTime = getBurnTime();
        int lastFuel = getLastFuelTime();
        if (burnTime <= 0 || lastFuel <= 0) {
            return 0;
        }
        return burnTime * 13 / lastFuel;
    }

    public int getScaledMiningProgress() {
        int progress = getMiningProgress();
        int total = getTicksPerBlock();
        if (total <= 0 || progress <= 0) {
            return 0;
        }
        return progress * 22 / total;
    }

    public boolean isBurning() {
        return getBurnTime() > 0;
    }

    public boolean hasPickaxe() {
        return !inventory.getItem(QuarryBlockEntity.PICKAXE_SLOT).isEmpty();
    }

    public boolean hasFuel() {
        return !inventory.getItem(QuarryBlockEntity.FUEL_SLOT).isEmpty();
    }

    public boolean hasValidFuel() {
        ItemStack fuel = inventory.getItem(QuarryBlockEntity.FUEL_SLOT);
        return !fuel.isEmpty() && blockEntity.getFuelValue(fuel) > 0;
    }

    public String getFilterModeText() {
        return switch (getFilterMode()) {
            case QuarryBlockEntity.FILTER_WHITELIST -> "Whitelist";
            case QuarryBlockEntity.FILTER_BLACKLIST -> "Blacklist";
            default -> "Off";
        };
    }

    // ==================== Custom Slot Classes ====================

    private static class PickaxeSlot extends Slot {
        private final QuarryBlockEntity quarry;

        PickaxeSlot(QuarryBlockEntity quarry, int index, int x, int y) {
            super(quarry, index, x, y);
            this.quarry = quarry;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return quarry.isValidPickaxe(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class FuelSlot extends Slot {
        private final QuarryBlockEntity quarry;

        FuelSlot(QuarryBlockEntity quarry, int index, int x, int y) {
            super(quarry, index, x, y);
            this.quarry = quarry;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return quarry.getFuelValue(stack) > 0;
        }
    }

    private static class OutputSlot extends Slot {
        OutputSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /**
     * Filter slot - accepts any item as a reference for filtering
     */
    private static class FilterSlot extends Slot {
        FilterSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true; // Accept any item as filter reference
        }

        @Override
        public int getMaxStackSize() {
            return 1; // Only need one item as reference
        }
    }
}
