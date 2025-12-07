package com.simplequarries.screen;

import com.simplequarries.SimpleQuarries;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Client-side screen for the Quarry GUI
 * 
 * Layout:
 * - Top 4 rows: Output grid (9x4 = 36 slots)
 * - Row 5: Pickaxe slot, Fuel slot, flame indicator, progress arrow
 * - Bottom: Player inventory
 */
public class QuarryScreen extends HandledScreen<QuarryScreenHandler> {
    
    // Use generic container as base texture (we'll draw slots over it)
    private static final Identifier CONTAINER_TEXTURE = Identifier.of("minecraft", "textures/gui/container/generic_54.png");
    
    // Furnace texture for flame and arrow sprites
    private static final Identifier FURNACE_TEXTURE = Identifier.of("minecraft", "textures/gui/container/furnace.png");

    // Container has 5 rows (4 rows output + 1 row for controls)
    private static final int CONTAINER_ROWS = 5;

    public QuarryScreen(QuarryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // Standard width, height for 5-row container + player inventory
        this.backgroundWidth = 176;
        this.backgroundHeight = 114 + CONTAINER_ROWS * 18; // 204 pixels
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Adjust title position
        titleX = 8;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw top part of container (header + 5 rows of slots)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, x, y, 0.0f, 0.0f, this.backgroundWidth, CONTAINER_ROWS * 18 + 17, 256, 256);
        
        // Draw bottom part (player inventory)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE, x, y + CONTAINER_ROWS * 18 + 17, 0.0f, 126.0f, this.backgroundWidth, 96, 256, 256);

        // Draw highlighted background for pickaxe slot (to make it obvious)
        // Pickaxe slot is at (8, 89) relative to GUI
        int pickaxeSlotX = x + 7;
        int pickaxeSlotY = y + 88;
        drawSlotHighlight(context, pickaxeSlotX, pickaxeSlotY, 0xFFAA6644); // Brown/copper tint for tool
        
        // Draw pickaxe icon hint if slot is empty
        if (!handler.hasPickaxe()) {
            context.drawItem(Items.WOODEN_PICKAXE.getDefaultStack(), pickaxeSlotX + 1, pickaxeSlotY + 1);
            // Draw semi-transparent overlay to indicate it's a hint
            context.fill(pickaxeSlotX + 1, pickaxeSlotY + 1, pickaxeSlotX + 17, pickaxeSlotY + 17, 0x80000000);
        }

        // Draw highlighted background for fuel slot
        // Fuel slot is at (26, 89) relative to GUI
        int fuelSlotX = x + 25;
        int fuelSlotY = y + 88;
        drawSlotHighlight(context, fuelSlotX, fuelSlotY, 0xFFDD6600); // Orange tint for fuel
        
        // Draw coal icon hint if slot is empty
        if (!handler.hasFuel()) {
            context.drawItem(Items.COAL.getDefaultStack(), fuelSlotX + 1, fuelSlotY + 1);
            // Draw semi-transparent overlay to indicate it's a hint
            context.fill(fuelSlotX + 1, fuelSlotY + 1, fuelSlotX + 17, fuelSlotY + 17, 0x80000000);
        }

        // Draw fuel flame (burning indicator) near the fuel slot
        if (handler.isBurning()) {
            int flame = handler.getScaledFuelProgress();
            // Flame sprite from furnace: 14x14 at (176, 0)
            int flameX = x + 45;
            int flameY = y + 93 + 12 - flame;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, flameX, flameY, 176.0f, (float)(12 - flame), 14, flame + 1, 256, 256);
        } else {
            // Draw empty flame outline
            context.drawTexture(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, x + 45, y + 93, 176.0f, 0.0f, 14, 13, 256, 256);
        }

        // Draw mining progress arrow
        int arrowX = x + 63;
        int arrowY = y + 91;
        // Draw empty arrow background first
        context.drawTexture(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, arrowX, arrowY, 79.0f, 35.0f, 24, 16, 256, 256);
        // Draw filled arrow progress
        int arrow = handler.getScaledMiningProgress();
        if (arrow > 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE, arrowX, arrowY, 176.0f, 14.0f, arrow + 1, 16, 256, 256);
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
        // Draw title
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        // Draw inventory label
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    /**
     * Draw a highlighted slot background with colored border
     */
    private void drawSlotHighlight(DrawContext context, int x, int y, int borderColor) {
        // Draw slot background (standard slot color)
        context.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        // Draw inner darker area (like vanilla slots)
        context.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        // Draw colored border on top and left (highlight)
        context.fill(x, y, x + 18, y + 1, borderColor); // Top
        context.fill(x, y, x + 1, y + 18, borderColor); // Left
        // Draw darker border on bottom and right
        int darkBorder = darkenColor(borderColor);
        context.fill(x, y + 17, x + 18, y + 18, darkBorder); // Bottom
        context.fill(x + 17, y, x + 18, y + 18, darkBorder); // Right
    }

    /**
     * Darken a color for the shadow edge of the slot
     */
    private int darkenColor(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, ((color >> 16) & 0xFF) - 80);
        int g = Math.max(0, ((color >> 8) & 0xFF) - 80);
        int b = Math.max(0, (color & 0xFF) - 80);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
