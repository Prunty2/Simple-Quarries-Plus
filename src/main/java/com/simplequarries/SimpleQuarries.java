package com.simplequarries;

import com.simplequarries.block.QuarryBlock;
import com.simplequarries.block.entity.QuarryBlockEntity;
import com.simplequarries.screen.QuarryScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleQuarries implements ModInitializer {
    public static final String MOD_ID = "simplequarries";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Declare fields - will be initialized in onInitialize
    public static Block QUARRY_BLOCK;
    public static Item QUARRY_BLOCK_ITEM;
    public static BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY;
    public static ScreenHandlerType<QuarryScreenHandler> QUARRY_SCREEN_HANDLER;

    @Override
    public void onInitialize() {
        // Create the block registry key
        Identifier quarryId = Identifier.of(MOD_ID, "quarry");
        RegistryKey<Block> quarryBlockKey = RegistryKey.of(RegistryKeys.BLOCK, quarryId);
        RegistryKey<Item> quarryItemKey = RegistryKey.of(RegistryKeys.ITEM, quarryId);

        // Register the Quarry block with registry key in settings
        QUARRY_BLOCK = Registry.register(
                Registries.BLOCK,
                quarryBlockKey,
                new QuarryBlock(AbstractBlock.Settings.create()
                        .registryKey(quarryBlockKey)
                        .strength(4.0f)
                        .requiresTool())
        );

        // Register the block item with registry key in settings
        QUARRY_BLOCK_ITEM = Registry.register(
                Registries.ITEM,
                quarryItemKey,
                new BlockItem(QUARRY_BLOCK, new Item.Settings().registryKey(quarryItemKey).useBlockPrefixedTranslationKey())
        );

        // Register the block entity type
        QUARRY_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(MOD_ID, "quarry"),
                FabricBlockEntityTypeBuilder.create(QuarryBlockEntity::new, QUARRY_BLOCK).build()
        );

        // Register the screen handler type using the new ExtendedScreenHandlerType with packet codec
        QUARRY_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(MOD_ID, "quarry"),
                new ExtendedScreenHandlerType<>(QuarryScreenHandler::new, QuarryScreenHandler.QuarryScreenData.PACKET_CODEC)
        );

        // Add to functional item group
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(QUARRY_BLOCK_ITEM));

        LOGGER.info("Simple Quarries loaded");
    }
}
