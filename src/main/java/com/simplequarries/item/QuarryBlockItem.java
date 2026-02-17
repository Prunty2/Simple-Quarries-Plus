package com.simplequarries.item;

import com.simplequarries.QuarryUpgrades;
import com.simplequarries.component.QuarryComponents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class QuarryBlockItem extends BlockItem {
    public QuarryBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    public static int getUpgradeCount(ItemStack stack) {
        int fromComponent = stack.getOrDefault(QuarryComponents.UPGRADE_COUNT, 0);
        if (fromComponent > 0) {
            return QuarryUpgrades.clampUpgradeCount(fromComponent);
        }
        // Fallback for stacks that only carry BlockEntityTag (e.g., drops)
        var blockEntityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            NbtCompound nbt = blockEntityData.copyNbtWithoutId();
            int fromTag = nbt.getInt("UpgradeCount").orElse(0);
            if (fromTag > 0) {
                return QuarryUpgrades.clampUpgradeCount(fromTag);
            }
        }
        return 0;
    }

    public static void setUpgradeCount(ItemStack stack, int count) {
        int clamped = QuarryUpgrades.clampUpgradeCount(count);
        stack.set(QuarryComponents.UPGRADE_COUNT, clamped);
    }

    public static int getSpeedUpgradeCount(ItemStack stack) {
        int fromComponent = stack.getOrDefault(QuarryComponents.SPEED_UPGRADE_COUNT, 0);
        if (fromComponent > 0) {
            return QuarryUpgrades.clampSpeedCount(fromComponent);
        }
        // Fallback for stacks that only carry BlockEntityTag (e.g., drops)
        var blockEntityData = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            NbtCompound nbt = blockEntityData.copyNbtWithoutId();
            int fromTag = nbt.getInt("SpeedUpgradeCount").orElse(0);
            if (fromTag > 0) {
                return QuarryUpgrades.clampSpeedCount(fromTag);
            }
        }
        return 0;
    }

    public static void setSpeedUpgradeCount(ItemStack stack, int count) {
        int clamped = QuarryUpgrades.clampSpeedCount(count);
        stack.set(QuarryComponents.SPEED_UPGRADE_COUNT, clamped);
    }

    public static int getMiningArea(ItemStack stack) {
        return QuarryUpgrades.areaForCount(getUpgradeCount(stack));
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent display, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, display, textConsumer, type);
        
        // Area upgrades
        int area = getMiningArea(stack);
        boolean areaAtMax = area >= QuarryUpgrades.MAX_AREA;
        if (areaAtMax) {
            textConsumer.accept(Text.empty()
                .append(Text.literal("Mining Area: ").formatted(Formatting.GRAY))
                .append(Text.literal(area + "x" + area).formatted(Formatting.GREEN))
                .append(Text.literal(" (Max)").formatted(Formatting.GOLD)));
        } else {
            textConsumer.accept(Text.empty()
                .append(Text.literal("Mining Area: ").formatted(Formatting.GRAY))
                .append(Text.literal(area + "x" + area).formatted(Formatting.GREEN)));
        }
        
        // Speed upgrades (always show)
        int speed = getSpeedUpgradeCount(stack);
        int percentBoost = (int) Math.round((1.0 - QuarryUpgrades.speedMultiplierForCount(speed)) * 100);
        boolean speedAtMax = speed >= QuarryUpgrades.MAX_SPEED_UPGRADES;
        textConsumer.accept(Text.empty()
            .append(Text.literal("Speed: ").formatted(Formatting.GRAY))
            .append(Text.literal("+" + percentBoost + "%").formatted(percentBoost > 0 ? Formatting.AQUA : Formatting.DARK_GRAY))
            .append(speedAtMax ? Text.literal(" (Max)").formatted(Formatting.GOLD) : Text.empty()));
    }
}