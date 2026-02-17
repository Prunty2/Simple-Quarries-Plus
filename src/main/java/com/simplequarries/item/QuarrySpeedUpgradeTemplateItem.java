package com.simplequarries.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class QuarrySpeedUpgradeTemplateItem extends Item {
    public QuarrySpeedUpgradeTemplateItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent display, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, display, textConsumer, type);
        textConsumer.accept(Text.empty()
            .append(Text.literal("⚡ ").formatted(Formatting.AQUA))
            .append(Text.translatable("tooltip.simplequarries.speed_template").formatted(Formatting.AQUA)));
    }
}
