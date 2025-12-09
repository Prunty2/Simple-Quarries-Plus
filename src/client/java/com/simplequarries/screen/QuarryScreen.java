package com.simplequarries.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Client-side screen for the Quarry GUI
 * 
 * Layout (furnace-style):
 * - Left: Pickaxe slot (top), Fuel slot (bottom) with flame
 * - Center: Progress arrow
 * - Right: 4x6 output grid (24 slots)
 * - Bottom: Player inventory
 */
public class QuarryScreen extends HandledScreen<QuarryScreenHandler> {
    
    // Furnace texture for the background and sprites
    private static final Identifier FURNACE_TEXTURE = Identifier.of("minecraft", "textures/gui/container/furnace.png");

    public QuarryScreen(QuarryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // Standard furnace-like dimensions
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleX = 8;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw the furnace background as base
        context.drawTexture(FURNACE_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, this.backgroundHeight, 256, 256);

        // Cover the furnace's default slots with background color, then draw our custom layout
        // Fill the area between arrow and output grid, and cover furnace slots
        context.fill(x + 26, y + 5, x + 61, y + 80, 0xFFC6C6C6);
        // Fill the furnace output slot area (the large slot that appears around x=116, y=35)
        context.fill(x + 108, y + 28, x + 138, y + 58, 0xFFC6C6C6);
        
        // Draw custom slot backgrounds for the output grid (4x6)
        // Visual slot background is 18x18, item renders at +1,+1 inside it
        int outputStartX = x + 61;
        int outputStartY = y + 7;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 6; col++) {
                int slotX = outputStartX + col * 18;
                int slotY = outputStartY + row * 18;
                drawSlot(context, slotX, slotY);
            }
        }

        // Draw pickaxe slot with highlight (at 8, 17)
        int pickaxeSlotX = x + 7;
        int pickaxeSlotY = y + 16;
        drawSlotHighlightCustom(context, pickaxeSlotX, pickaxeSlotY, 0xFFAA7744); // Brown tint
        
        // Draw pickaxe icon hint if slot is empty
        if (!handler.hasPickaxe()) {
            context.drawItem(Items.IRON_PICKAXE.getDefaultStack(), pickaxeSlotX + 1, pickaxeSlotY + 1);
            context.fill(pickaxeSlotX + 1, pickaxeSlotY + 1, pickaxeSlotX + 17, pickaxeSlotY + 17, 0x80000000);
        }

        // Draw fuel slot with highlight (at 8, 53)
        int fuelSlotX = x + 7;
        int fuelSlotY = y + 52;
        drawSlotHighlightCustom(context, fuelSlotX, fuelSlotY, 0xFFDD6600); // Orange tint
        
        // Draw coal icon hint if slot is empty
        if (!handler.hasFuel()) {
            context.drawItem(Items.COAL.getDefaultStack(), fuelSlotX + 1, fuelSlotY + 1);
            context.fill(fuelSlotX + 1, fuelSlotY + 1, fuelSlotX + 17, fuelSlotY + 17, 0x80000000);
        }

        // Draw fuel flame indicator (between pickaxe and fuel slots)
        int flameX = x + 9;
        int flameY = y + 36;
        if (handler.isBurning()) {
            int flame = handler.getScaledFuelProgress();
            context.drawTexture(FURNACE_TEXTURE, flameX, flameY + 12 - flame, 176.0f, (float)(12 - flame), 14, flame + 1, 256, 256);
        }

        // Draw progress arrow (pointing right toward output)
        int arrowX = x + 34;
        int arrowY = y + 34;
        // Draw empty arrow background
        context.drawTexture(FURNACE_TEXTURE, arrowX, arrowY, 79.0f, 35.0f, 24, 16, 256, 256);
        // Draw filled arrow progress
        int arrow = handler.getScaledMiningProgress();
        if (arrow > 0) {
            context.drawTexture(FURNACE_TEXTURE, arrowX, arrowY, 176.0f, 14.0f, arrow + 1, 16, 256, 256);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    /**
     * Draw a standard slot background
     */
    private void drawSlot(DrawContext context, int x, int y) {
        // Outer border (dark on top-left, light on bottom-right for 3D effect)
        context.fill(x, y, x + 18, y + 1, 0xFF373737);      // Top dark
        context.fill(x, y, x + 1, y + 18, 0xFF373737);      // Left dark
        context.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF); // Right light
        context.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF); // Bottom light
        // Inner slot area (dark)
        context.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    /**
     * Draw a highlighted slot background with colored border
     */
    private void drawSlotHighlightCustom(DrawContext context, int x, int y, int borderColor) {
        // Draw colored border
        context.fill(x, y, x + 18, y + 2, borderColor);      // Top
        context.fill(x, y, x + 2, y + 18, borderColor);      // Left
        context.fill(x + 16, y, x + 18, y + 18, darkenColor(borderColor)); // Right
        context.fill(x, y + 16, x + 18, y + 18, darkenColor(borderColor)); // Bottom
        // Inner slot area
        context.fill(x + 2, y + 2, x + 16, y + 16, 0xFF8B8B8B);
    }

    /**
     * Darken a color for the shadow edge
     */
    private int darkenColor(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - 60);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 60);
        int b = Math.max(0, (color & 0xFF) - 60);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
