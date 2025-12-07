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
     * Client-side constructor - called when opening the GUI from the server
     */
    public QuarryScreenHandler(int syncId, PlayerInventory playerInventory, QuarryScreenData data) {
        this(syncId, playerInventory, getBlockEntity(playerInventory, data.pos()), new ArrayPropertyDelegate(4));
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

        // Add output slots (4 rows x 9 columns = 36 slots, indices 2-37)
        // Start at X=8 to align with player inventory below
        int outputStartX = 8;
        int outputStartY = 17;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = QuarryBlockEntity.OUTPUT_START + row * 9 + col;
                int x = outputStartX + col * 18;
                int y = outputStartY + row * 18;
                this.addSlot(new OutputSlot(blockEntity, slotIndex, x, y));
            }
        }

        // Add pickaxe slot (slot 0) - in 5th row, first position
        int row5Y = outputStartY + 4 * 18; // 17 + 72 = 89
        this.addSlot(new PickaxeSlot(blockEntity, QuarryBlockEntity.PICKAXE_SLOT, 8, row5Y));
        
        // Add fuel slot (slot 1) - in 5th row, second position
        this.addSlot(new FuelSlot(blockEntity, QuarryBlockEntity.FUEL_SLOT, 26, row5Y));

        // Add player inventory (3 rows x 9 columns)
        int playerInvY = 103;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }

        // Add player hotbar
        int hotbarY = 161;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }

        // Add property delegate for syncing burn time, etc.
        addProperties(propertyDelegate);
    }

    /**
     * Get the block entity from the world on the client side
     */
    private static QuarryBlockEntity getBlockEntity(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getEntityWorld().getBlockEntity(pos) instanceof QuarryBlockEntity quarry) {
            return quarry;
        }
        throw new IllegalStateException("Quarry block entity not found at " + pos);
    }

    /**
     * Handle shift-clicking items between slots
     */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();

            int quarrySlotCount = QuarryBlockEntity.INVENTORY_SIZE;
            int playerSlotStart = quarrySlotCount;
            int playerSlotEnd = playerSlotStart + 36;

            if (slotIndex < quarrySlotCount) {
                // Moving from quarry to player inventory
                if (!this.insertItem(original, playerSlotStart, playerSlotEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to quarry
                if (blockEntity.isValidPickaxe(original)) {
                    // Try to insert into pickaxe slot
                    if (!this.insertItem(original, QuarryBlockEntity.PICKAXE_SLOT, QuarryBlockEntity.PICKAXE_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (blockEntity.getFuelValue(original) > 0) {
                    // Try to insert into fuel slot
                    if (!this.insertItem(original, QuarryBlockEntity.FUEL_SLOT, QuarryBlockEntity.FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // No valid slot for this item
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

    // ==================== Property Getters for GUI ====================

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

    /**
     * Get fuel progress as a scaled value (0-13) for the flame icon
     */
    public int getScaledFuelProgress() {
        int burnTime = getBurnTime();
        int lastFuel = getLastFuelTime();
        if (burnTime <= 0 || lastFuel <= 0) {
            return 0;
        }
        return burnTime * 13 / lastFuel;
    }

    /**
     * Get mining progress as a scaled value (0-22) for the arrow icon
     */
    public int getScaledMiningProgress() {
        int progress = getMiningProgress();
        int total = getTicksPerBlock();
        if (total <= 0 || progress <= 0) {
            return 0;
        }
        return progress * 22 / total;
    }

    /**
     * Check if the quarry is currently burning fuel
     */
    public boolean isBurning() {
        return getBurnTime() > 0;
    }

    /**
     * Check if the pickaxe slot has a pickaxe
     */
    public boolean hasPickaxe() {
        return !inventory.getStack(QuarryBlockEntity.PICKAXE_SLOT).isEmpty();
    }

    /**
     * Check if the fuel slot has fuel
     */
    public boolean hasFuel() {
        return !inventory.getStack(QuarryBlockEntity.FUEL_SLOT).isEmpty();
    }

    // ==================== Custom Slot Classes ====================

    /**
     * Slot that only accepts valid pickaxes
     */
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

    /**
     * Slot that only accepts fuel items
     */
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

    /**
     * Output slot - items can only be taken out, not inserted
     */
    private static class OutputSlot extends Slot {
        OutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false; // Output slots don't accept items
        }
    }
}
