package com.simplequarries;

import com.simplequarries.item.QuarryBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class QuarrySpeedUpgradeRecipe extends SpecialCraftingRecipe {
    public QuarrySpeedUpgradeRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        ItemStack quarryStack = ItemStack.EMPTY;
        int templateCount = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.isOf(SimpleQuarries.QUARRY_BLOCK_ITEM)) {
                if (!quarryStack.isEmpty()) {
                    return false; // Multiple quarries present
                }
                quarryStack = stack;
            } else if (stack.isOf(SimpleQuarries.QUARRY_SPEED_UPGRADE_TEMPLATE)) {
                templateCount++;
                if (templateCount > 1) {
                    return false; // Only one template allowed
                }
            } else {
                return false; // Unknown ingredient
            }
        }

        if (quarryStack.isEmpty() || templateCount != 1) {
            return false;
        }

        int currentSpeedUpgrades = QuarryBlockItem.getSpeedUpgradeCount(quarryStack);
        return currentSpeedUpgrades < QuarryUpgrades.MAX_SPEED_UPGRADES;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup registries) {
        ItemStack quarryStack = ItemStack.EMPTY;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.isOf(SimpleQuarries.QUARRY_BLOCK_ITEM)) {
                quarryStack = stack;
                break;
            }
        }

        if (quarryStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Keep area upgrades, add speed upgrade
        int currentAreaUpgrades = QuarryBlockItem.getUpgradeCount(quarryStack);
        
        ItemStack result = quarryStack.copy();
        result.setCount(1);
        
        // Set area upgrades (keep existing)
        QuarryBlockItem.setUpgradeCount(result, currentAreaUpgrades);
        
        // Add speed upgrade
        int upgradedSpeed = QuarryBlockItem.getSpeedUpgradeCount(quarryStack) + 1;
        QuarryBlockItem.setSpeedUpgradeCount(result, upgradedSpeed);
        
        return result;
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return SimpleQuarries.QUARRY_SPEED_UPGRADE_RECIPE_SERIALIZER;
    }

    public DefaultedList<net.minecraft.recipe.Ingredient> getIngredients() {
        DefaultedList<Ingredient> ingredients = DefaultedList.of();
        ingredients.add(Ingredient.ofItems(SimpleQuarries.QUARRY_BLOCK_ITEM));
        ingredients.add(Ingredient.ofItems(SimpleQuarries.QUARRY_SPEED_UPGRADE_TEMPLATE));
        return ingredients;
    }

    @Override
    public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput input) {
        return DefaultedList.ofSize(input.size(), ItemStack.EMPTY);
    }
}