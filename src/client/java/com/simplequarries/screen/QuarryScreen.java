package com.simplequarries.screen;

import com.simplequarries.block.entity.QuarryBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

/**
 * Client-side screen for the Quarry GUI — spacious layout
 *
 * Layout (top to bottom):
 *   Row 1: [Pickaxe] [Flame] [Arrow]  |  [4x6 Output Grid]
 *   ---separator---
 *   Row 2: [Filter Button]  |  [3x3 Filter Grid]
 *   ---separator---
 *   Row 3: Player inventory (3 rows + hotbar)
 */
public class QuarryScreen extends AbstractContainerScreen<QuarryScreenHandler> {

    private static final Identifier FURNACE_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");

    private static final int BG       = 0xFFC6C6C6;
    private static final int LIGHT    = 0xFFFFFFFF;
    private static final int DARK     = 0xFF555555;
    private static final int DARKER   = 0xFF373737;
    private static final int SLOT_BG  = 0xFF8B8B8B;
    private static final int SEP      = 0xFFA0A0A0;
    private static final int TXT      = 4210752;

    private Button filterButton;

    public QuarryScreen(QuarryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 244);
        this.inventoryLabelY = 146;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        filterButton = Button.builder(
                getFilterText(),
                btn -> {
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                }
        ).bounds(x + 8, y + 104, 88, 20).build();
        this.addRenderableWidget(filterButton);
    }

    private Component getFilterText() {
        return switch (menu.getFilterMode()) {
            case QuarryBlockEntity.FILTER_WHITELIST -> Component.literal("§aFilter: Whitelist");
            case QuarryBlockEntity.FILTER_BLACKLIST -> Component.literal("§cFilter: Blacklist");
            default -> Component.literal("§7Filter: Disabled");
        };
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (filterButton != null) filterButton.setMessage(getFilterText());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractBackground(ctx, mouseX, mouseY, delta);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ── Background panel ──
        drawPanel(ctx, x, y, imageWidth, imageHeight);

        // ══════════ TOP: Mining section ══════════

        // Output grid 4×6
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 6; c++)
                drawSlot(ctx, x + 61 + c * 18, y + 9 + r * 18);

        // Pickaxe slot
        int px = x + 9, py = y + 19;
        drawSlot(ctx, px, py);
        if (!menu.hasPickaxe()) {
            ctx.item(Items.IRON_PICKAXE.getDefaultInstance(), px + 1, py + 1);
            ctx.fill(px + 1, py + 1, px + 17, py + 17, 0x80C6C6C6);
        }

        // Fuel slot
        int fx = x + 9, fy = y + 57;
        drawSlot(ctx, fx, fy);
        if (!menu.hasFuel()) {
            ctx.item(Items.COAL.getDefaultInstance(), fx + 1, fy + 1);
            ctx.fill(fx + 1, fy + 1, fx + 17, fy + 17, 0x80C6C6C6);
        }

        // Flame
        if (menu.isBurning()) {
            int fl = menu.getScaledFuelProgress();
            ctx.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
                    x + 11, y + 41 + 12 - fl, 176f, (float)(12 - fl), 14, fl + 1, 256, 256);
        }

        // Arrow
        ctx.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
                x + 35, y + 38, 79f, 35f, 24, 16, 256, 256);
        int arrow = menu.getScaledMiningProgress();
        if (arrow > 0)
            ctx.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
                    x + 35, y + 38, 176f, 14f, arrow + 1, 16, 256, 256);

        // ── Fuel counter ──
        {
            int fuel = menu.getBurnTime();
            boolean hasFuelItem = menu.hasValidFuel();
            String fuelStr;
            int barColor;
            if (fuel > 0) {
                fuelStr = fuel + " blocks";
                barColor = 0xFF44AA44; // green
            } else if (hasFuelItem) {
                fuelStr = "Ready";
                barColor = 0xFFAAAA22; // yellow
            } else {
                fuelStr = "Empty";
                barColor = 0xFFAA4444; // red
            }
            // Draw below the fuel slot, left-aligned with it
            ctx.text(font, Component.literal(fuelStr), x + 8, y + 78, barColor, true);
        }

        // ── Separator ──
        drawSep(ctx, x + 7, y + 90, 162);

        // ══════════ MIDDLE: Filter section ══════════

        // "Filter Items" label
        ctx.text(font, Component.literal("Filter Items"), x + 100, y + 88, TXT, false);

        // 3×3 filter grid
        int accentColor = getFilterAccent();
        int gx = x + 99, gy = y + 95;

        // Subtle tinted background when filter is active
        if (menu.getFilterMode() != QuarryBlockEntity.FILTER_DISABLED)
            ctx.fill(gx - 1, gy - 1, gx + 55, gy + 55, (accentColor & 0x00FFFFFF) | 0x20000000);

        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                drawSlot(ctx, gx + c * 18, gy + r * 18);

        // ── Separator ──
        drawSep(ctx, x + 7, y + 152, 162);

        // ══════════ BOTTOM: Player inventory ══════════
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                drawSlot(ctx, x + 7 + c * 18, y + 157 + r * 18);

        // Hotbar (4px gap)
        for (int c = 0; c < 9; c++)
            drawSlot(ctx, x + 7 + c * 18, y + 219);
    }

    private int getFilterAccent() {
        return switch (menu.getFilterMode()) {
            case QuarryBlockEntity.FILTER_WHITELIST -> 0xFF44BB44;
            case QuarryBlockEntity.FILTER_BLACKLIST -> 0xFFBB4444;
            default -> 0xFF888888;
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        ctx.text(font, title, titleLabelX, titleLabelY, TXT, false);
        ctx.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TXT, false);

        // Fuel display is drawn in drawBackground
    }

    // ── Drawing helpers ──

    private void drawPanel(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, BG);
        ctx.fill(x, y, x + w, y + 2, LIGHT);
        ctx.fill(x, y, x + 2, y + h, LIGHT);
        ctx.fill(x + w - 2, y, x + w, y + h, DARK);
        ctx.fill(x, y + h - 2, x + w, y + h, DARK);
    }

    private void drawSlot(GuiGraphicsExtractor ctx, int x, int y) {
        ctx.fill(x, y, x + 18, y + 1, DARKER);
        ctx.fill(x, y, x + 1, y + 18, DARKER);
        ctx.fill(x + 17, y + 1, x + 18, y + 18, LIGHT);
        ctx.fill(x + 1, y + 17, x + 17, y + 18, LIGHT);
        ctx.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    private void drawSep(GuiGraphicsExtractor ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, SEP);
        ctx.fill(x, y + 1, x + w, y + 2, LIGHT);
    }
}
