package com.simplequarries.screen;

import com.simplequarries.SimpleQuarries;
import com.simplequarries.block.entity.QuarryBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Screen handler for the Quarry GUI
 */
public class QuarryScreenHandler extends ScreenHandler {
    
    private final Inventory inventory;
    private final QuarryBlockEntity blockEntity;
    private final PropertyDelegate propertyDelegate;

    /**
     * Data sent from server to client when opening the screen
     */
    public record QuarryScreenData(BlockPos pos) {
        public static final PacketCodec<RegistryByteBuf, QuarryScreenData> PACKET_CODEC = PacketCodec.of(
                (data, buf) -> buf.writeBlockPos(data.pos),
                buf -> new QuarryScreenData(buf.readBlockPos())
        );
    }

    /**
     * Client-side constructor
     */
    public QuarryScreenHandler(int syncId, PlayerInventory playerInventory, QuarryScreenData data) {
        this(syncId, playerInventory, getBlockEntity(playerInventory, data.pos()), new ArrayPropertyDelegate(6));
    }

    /**
     * Server-side constructor
     */
    public QuarryScreenHandler(int syncId, PlayerInventory playerInventory, QuarryBlockEntity blockEntity, PropertyDelegate propertyDelegate) {
        super(SimpleQuarries.QUARRY_SCREEN_HANDLER, syncId);
        
        this.blockEntity = blockEntity;
        this.inventory = blockEntity;
        this.propertyDelegate = propertyDelegate;

        checkSize(inventory, QuarryBlockEntity.INVENTORY_SIZE);
        inventory.onOpen(playerInventory.player);

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

        addProperties(propertyDelegate);
    }

    private static QuarryBlockEntity getBlockEntity(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getEntityWorld().getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
            return quarry;
        }
        throw new IllegalStateException("Quarry block entity not found at " + pos);
    }

    // Total quarry slots: pickaxe(1) + fuel(1) + output(24) + filter(9) = 35
    private static final int QUARRY_SLOT_COUNT = QuarryBlockEntity.INVENTORY_SIZE;

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();

            int playerSlotStart = QUARRY_SLOT_COUNT;
            int playerSlotEnd = playerSlotStart + 36;

            if (slotIndex < QUARRY_SLOT_COUNT) {
                // Moving from quarry to player inventory
                if (!this.insertItem(original, playerSlotStart, playerSlotEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to quarry
                if (blockEntity.isValidPickaxe(original)) {
                    if (!this.insertItem(original, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (blockEntity.getFuelValue(original) > 0) {
                    if (!this.insertItem(original, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    /**
     * Handle button clicks from the client (filter mode toggle, chunk loader toggle)
     */
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
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
        return !inventory.getStack(QuarryBlockEntity.PICKAXE_SLOT).isEmpty();
    }

    public boolean hasFuel() {
        return !inventory.getStack(QuarryBlockEntity.FUEL_SLOT).isEmpty();
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
        public boolean canInsert(ItemStack stack) {
            return quarry.isValidPickaxe(stack);
        }

        @Override
        public int getMaxItemCount() {
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
        public boolean canInsert(ItemStack stack) {
            return quarry.getFuelValue(stack) > 0;
        }
    }

    private static class OutputSlot extends Slot {
        OutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }

    /**
     * Filter slot - accepts any item as a reference for filtering
     */
    private static class FilterSlot extends Slot {
        FilterSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return true; // Accept any item as filter reference
        }

        @Override
        public int getMaxItemCount() {
            return 1; // Only need one item as reference
        }
    }
}
