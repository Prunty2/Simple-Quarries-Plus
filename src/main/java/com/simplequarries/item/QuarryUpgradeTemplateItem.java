package com.simplequarries.item;

import com.simplequarries.QuarryUpgrades;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class QuarryUpgradeTemplateItem extends Item {
    public QuarryUpgradeTemplateItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent display, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, display, textConsumer, type);
        textConsumer.accept(Text.empty()
            .append(Text.literal("📐 ").formatted(Formatting.GREEN))
            .append(Text.literal("Expands mining area by " + QuarryUpgrades.AREA_UPGRADE_STEP + " blocks").formatted(Formatting.GREEN)));
    }
}
