package com.simplequarries.item;

import com.simplequarries.QuarryUpgrades;
import com.simplequarries.component.QuarryComponents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class QuarryBlockItem extends BlockItem {
    public QuarryBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    public static int getUpgradeCount(ItemStack stack) {
        int fromComponent = stack.getOrDefault(QuarryComponents.UPGRADE_COUNT, 0);
        return QuarryUpgrades.clampUpgradeCount(fromComponent);
    }

    public static void setUpgradeCount(ItemStack stack, int count) {
        int clamped = QuarryUpgrades.clampUpgradeCount(count);
        stack.set(QuarryComponents.UPGRADE_COUNT, clamped);
    }

    public static int getMiningArea(ItemStack stack) {
        return QuarryUpgrades.areaForCount(getUpgradeCount(stack));
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        int area = getMiningArea(stack);
        boolean atMax = area >= QuarryUpgrades.MAX_AREA;
        if (atMax) {
            tooltip.add(Text.translatable("tooltip.simplequarries.quarry.area_max", area, area));
        } else {
            tooltip.add(Text.translatable("tooltip.simplequarries.quarry.area", area, area));
        }
    }
}
