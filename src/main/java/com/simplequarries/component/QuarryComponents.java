package com.simplequarries.component;

import com.mojang.serialization.Codec;
import com.simplequarries.SimpleQuarries;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

public final class QuarryComponents {
    private QuarryComponents() {}

    public static DataComponentType<Integer> UPGRADE_COUNT;
    public static DataComponentType<Integer> SPEED_UPGRADE_COUNT;

    public static void register() {
        UPGRADE_COUNT = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SimpleQuarries.MOD_ID, "quarry_upgrade_count"),
                DataComponentType.<Integer>builder()
                        .persistent(Codec.INT)
                        .networkSynchronized(ByteBufCodecs.VAR_INT)
                        .build()
        );

        SPEED_UPGRADE_COUNT = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SimpleQuarries.MOD_ID, "quarry_speed_upgrade_count"),
                DataComponentType.<Integer>builder()
                        .persistent(Codec.INT)
                        .networkSynchronized(ByteBufCodecs.VAR_INT)
                        .build()
        );
    }
}
