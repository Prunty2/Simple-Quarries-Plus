package com.simplequarries.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class QuarrySpeedUpgradeTemplateItem extends Item {
    public QuarrySpeedUpgradeTemplateItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, display, textConsumer, type);
        textConsumer.accept(Component.empty()
            .append(Component.literal("⚡ ").withStyle(ChatFormatting.AQUA))
            .append(Component.translatable("tooltip.simplequarries.speed_template").withStyle(ChatFormatting.AQUA)));
    }
}
