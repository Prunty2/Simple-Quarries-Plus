package com.simplequarries.component;

import com.mojang.serialization.Codec;
import com.simplequarries.SimpleQuarries;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class QuarryComponents {
    private QuarryComponents() {}

    public static ComponentType<Integer> UPGRADE_COUNT;
    public static ComponentType<Integer> SPEED_UPGRADE_COUNT;

    public static void register() {
        UPGRADE_COUNT = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(SimpleQuarries.MOD_ID, "quarry_upgrade_count"),
                ComponentType.<Integer>builder()
                        .codec(Codec.INT)
                        .packetCodec(PacketCodecs.VAR_INT)
                        .build()
        );

        SPEED_UPGRADE_COUNT = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(SimpleQuarries.MOD_ID, "quarry_speed_upgrade_count"),
                ComponentType.<Integer>builder()
                        .codec(Codec.INT)
                        .packetCodec(PacketCodecs.VAR_INT)
                        .build()
        );
    }
}