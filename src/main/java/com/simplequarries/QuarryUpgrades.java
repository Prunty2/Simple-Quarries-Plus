package com.simplequarries;

import net.minecraft.util.math.MathHelper;

public final class QuarryUpgrades {
    private QuarryUpgrades() {}

    // Area upgrades
    public static final int BASE_AREA = 5;
    public static final int MAX_AREA = 15;
    public static final int AREA_UPGRADE_STEP = 2;
    public static final int MAX_AREA_UPGRADES = (MAX_AREA - BASE_AREA) / AREA_UPGRADE_STEP; // 5 upgrades -> 15x15

    // Speed upgrades
    public static final int MAX_SPEED_UPGRADES = 5;
    private static final double[] SPEED_MULTIPLIERS = {
        1.0,      // Base (no upgrades)
        0.8,      // -20% speed
        0.6,      // -40% speed
        0.4,      // -60% speed
        0.2,      // -80% speed
        0.0       // -100% speed (instant)
    };

    public static int clampUpgradeCount(int value) {
        return MathHelper.clamp(value, 0, MAX_AREA_UPGRADES);
    }

    public static int clampSpeedCount(int value) {
        return MathHelper.clamp(value, 0, MAX_SPEED_UPGRADES);
    }

    public static int areaForCount(int upgradeCount) {
        return Math.min(MAX_AREA, BASE_AREA + (clampUpgradeCount(upgradeCount) * AREA_UPGRADE_STEP));
    }

    public static double speedMultiplierForCount(int upgradeCount) {
        int count = clampSpeedCount(upgradeCount);
        return SPEED_MULTIPLIERS[count];
    }

    public static int getMaxAreaUpgrades() {
        return MAX_AREA_UPGRADES;
    }

    public static int getMaxSpeedUpgrades() {
        return MAX_SPEED_UPGRADES;
    }
}