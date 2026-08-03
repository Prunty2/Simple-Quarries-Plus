package com.simplequarries.item;

import com.simplequarries.QuarryUpgrades;
import com.simplequarries.component.QuarryComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class QuarryBlockItem extends BlockItem {
    public QuarryBlockItem(Block block, Properties settings) {
        super(block, settings);
    }

    public static int getUpgradeCount(ItemStack stack) {
        int fromComponent = stack.getOrDefault(QuarryComponents.UPGRADE_COUNT, 0);
        if (fromComponent > 0) {
            return QuarryUpgrades.clampUpgradeCount(fromComponent);
        }
        // Fallback for stacks that only carry BlockEntityTag (e.g., drops)
        var blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTagWithoutId();
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
        var blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            CompoundTag nbt = blockEntityData.copyTagWithoutId();
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, display, textConsumer, type);
        
        // Area upgrades
        int area = getMiningArea(stack);
        boolean areaAtMax = area >= QuarryUpgrades.MAX_AREA;
        if (areaAtMax) {
            textConsumer.accept(Component.empty()
                .append(Component.literal("Mining Area: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(area + "x" + area).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" (Max)").withStyle(ChatFormatting.GOLD)));
        } else {
            textConsumer.accept(Component.empty()
                .append(Component.literal("Mining Area: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(area + "x" + area).withStyle(ChatFormatting.GREEN)));
        }
        
        // Speed upgrades (always show)
        int speed = getSpeedUpgradeCount(stack);
        int percentBoost = (int) Math.round((1.0 - QuarryUpgrades.speedMultiplierForCount(speed)) * 100);
        boolean speedAtMax = speed >= QuarryUpgrades.MAX_SPEED_UPGRADES;
        textConsumer.accept(Component.empty()
            .append(Component.literal("Speed: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("+" + percentBoost + "%").withStyle(percentBoost > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY))
            .append(speedAtMax ? Component.literal(" (Max)").withStyle(ChatFormatting.GOLD) : Component.empty()));
    }
}