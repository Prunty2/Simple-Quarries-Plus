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
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
    public static MenuType<QuarryScreenHandler> QUARRY_SCREEN_HANDLER;
    public static RecipeSerializer<QuarryUpgradeRecipe> QUARRY_UPGRADE_RECIPE_SERIALIZER;
    public static RecipeSerializer<QuarrySpeedUpgradeRecipe> QUARRY_SPEED_UPGRADE_RECIPE_SERIALIZER;

    @Override
    public void onInitialize() {
        QuarryComponents.register();

        // Create the block registry key
        Identifier quarryId = Identifier.fromNamespaceAndPath(MOD_ID, "quarry");
        ResourceKey<Block> quarryBlockKey = ResourceKey.create(Registries.BLOCK, quarryId);
        ResourceKey<Item> quarryItemKey = ResourceKey.create(Registries.ITEM, quarryId);

        // Register the Quarry block with registry key in settings
        QUARRY_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK,
                quarryBlockKey,
                new QuarryBlock(BlockBehaviour.Properties.of()
                        .setId(quarryBlockKey)
                        .strength(4.0f)
                        .requiresCorrectToolForDrops())
        );

        // Register the block item with registry key in settings
        QUARRY_BLOCK_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                quarryItemKey,
                new QuarryBlockItem(QUARRY_BLOCK, new Item.Properties().setId(quarryItemKey).useBlockDescriptionPrefix())
        );

        // Register the area upgrade template item
        Identifier templateId = Identifier.fromNamespaceAndPath(MOD_ID, "quarry_upgrade_template");
        ResourceKey<Item> templateKey = ResourceKey.create(Registries.ITEM, templateId);
        QUARRY_UPGRADE_TEMPLATE = Registry.register(
                BuiltInRegistries.ITEM,
                templateKey,
                new QuarryUpgradeTemplateItem(new Item.Properties().setId(templateKey))
        );

        // Register the speed upgrade template item
        Identifier speedTemplateId = Identifier.fromNamespaceAndPath(MOD_ID, "quarry_speed_upgrade_template");
        ResourceKey<Item> speedTemplateKey = ResourceKey.create(Registries.ITEM, speedTemplateId);
        QUARRY_SPEED_UPGRADE_TEMPLATE = Registry.register(
                BuiltInRegistries.ITEM,
                speedTemplateKey,
                new QuarrySpeedUpgradeTemplateItem(new Item.Properties().setId(speedTemplateKey))
        );

        // Register the block entity type
        QUARRY_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "quarry"),
                FabricBlockEntityTypeBuilder.create(QuarryBlockEntity::new, QUARRY_BLOCK).build()
        );

        // Register the screen handler type using the new ExtendedMenuType with packet codec
        QUARRY_SCREEN_HANDLER = Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(MOD_ID, "quarry"),
                new ExtendedMenuType<>(QuarryScreenHandler::new, QuarryScreenHandler.QuarryScreenData.PACKET_CODEC)
        );

        QUARRY_UPGRADE_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "quarry_upgrade"),
                new RecipeSerializer<>(
                        MapCodec.unit(QuarryUpgradeRecipe::new),
                        StreamCodec.<RegistryFriendlyByteBuf, QuarryUpgradeRecipe>unit(new QuarryUpgradeRecipe())
                )
        );

        QUARRY_SPEED_UPGRADE_RECIPE_SERIALIZER = Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "quarry_speed_upgrade"),
                new RecipeSerializer<>(
                        MapCodec.unit(QuarrySpeedUpgradeRecipe::new),
                        StreamCodec.<RegistryFriendlyByteBuf, QuarrySpeedUpgradeRecipe>unit(new QuarrySpeedUpgradeRecipe())
                )
        );

        // Add to functional item group
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(QUARRY_BLOCK_ITEM);
            entries.accept(QUARRY_UPGRADE_TEMPLATE);
            entries.accept(QUARRY_SPEED_UPGRADE_TEMPLATE);
        });

        QuarryLootInjectors.register();
        LOGGER.info("Simple Quarries loaded");
    }
}
