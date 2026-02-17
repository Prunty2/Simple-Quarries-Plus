package com.simplequarries;

import com.simplequarries.block.QuarryBlock;
import com.simplequarries.block.entity.QuarryBlockEntity;
import com.simplequarries.component.QuarryComponents;
import com.simplequarries.item.QuarryBlockItem;
import com.simplequarries.item.QuarrySpeedUpgradeTemplateItem;
import com.simplequarries.item.QuarryUpgradeTemplateItem;
import com.simplequarries.recipe.QuarryUpgradeRecipe;
import com.simplequarries.loot.QuarryLootInjectors;
import com.simplequarries.screen.QuarryScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
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
    public static QuarryBlockItem QUARRY_BLOCK_ITEM;
    public static Item QUARRY_UPGRADE_TEMPLATE;
    public static Item QUARRY_SPEED_UPGRADE_TEMPLATE;
    public static BlockEntityType<QuarryBlockEntity> QUARRY_BLOCK_ENTITY;
    public static ScreenHandlerType<QuarryScreenHandler> QUARRY_SCREEN_HANDLER;
    public static RecipeSerializer<QuarryUpgradeRecipe> QUARRY_UPGRADE_RECIPE_SERIALIZER;
    public static RecipeSerializer<QuarrySpeedUpgradeRecipe> QUARRY_SPEED_UPGRADE_RECIPE_SERIALIZER;

    @Override
    public void onInitialize() {
        QuarryComponents.register();

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
                new QuarryBlockItem(QUARRY_BLOCK, new Item.Settings().registryKey(quarryItemKey).useBlockPrefixedTranslationKey())
        );

        // Register the area upgrade template item
        Identifier templateId = Identifier.of(MOD_ID, "quarry_upgrade_template");
        RegistryKey<Item> templateKey = RegistryKey.of(RegistryKeys.ITEM, templateId);
        QUARRY_UPGRADE_TEMPLATE = Registry.register(
                Registries.ITEM,
                templateKey,
                new QuarryUpgradeTemplateItem(new Item.Settings().registryKey(templateKey))
        );

        // Register the speed upgrade template item
        Identifier speedTemplateId = Identifier.of(MOD_ID, "quarry_speed_upgrade_template");
        RegistryKey<Item> speedTemplateKey = RegistryKey.of(RegistryKeys.ITEM, speedTemplateId);
        QUARRY_SPEED_UPGRADE_TEMPLATE = Registry.register(
                Registries.ITEM,
                speedTemplateKey,
                new QuarrySpeedUpgradeTemplateItem(new Item.Settings().registryKey(speedTemplateKey))
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

        QUARRY_UPGRADE_RECIPE_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(MOD_ID, "quarry_upgrade"),
                new SpecialCraftingRecipe.SpecialRecipeSerializer<>(QuarryUpgradeRecipe::new)
        );

        QUARRY_SPEED_UPGRADE_RECIPE_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(MOD_ID, "quarry_speed_upgrade"),
                new SpecialCraftingRecipe.SpecialRecipeSerializer<>(QuarrySpeedUpgradeRecipe::new)
        );

        // Add to functional item group
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(QUARRY_BLOCK_ITEM);
            entries.add(QUARRY_UPGRADE_TEMPLATE);
            entries.add(QUARRY_SPEED_UPGRADE_TEMPLATE);
        });

        QuarryLootInjectors.register();
        LOGGER.info("Simple Quarries loaded");
    }
}