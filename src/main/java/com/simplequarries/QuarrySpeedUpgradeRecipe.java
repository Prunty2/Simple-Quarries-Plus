package com.simplequarries;

import com.simplequarries.item.QuarryBlockItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class QuarrySpeedUpgradeRecipe extends CustomRecipe {
    public QuarrySpeedUpgradeRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        ItemStack quarryStack = ItemStack.EMPTY;
        int templateCount = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(SimpleQuarries.QUARRY_BLOCK_ITEM)) {
                if (!quarryStack.isEmpty()) {
                    return false; // Multiple quarries present
                }
                quarryStack = stack;
            } else if (stack.is(SimpleQuarries.QUARRY_SPEED_UPGRADE_TEMPLATE)) {
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
    public ItemStack assemble(CraftingInput inventory) {
        ItemStack quarryStack = ItemStack.EMPTY;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(SimpleQuarries.QUARRY_BLOCK_ITEM)) {
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
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SimpleQuarries.QUARRY_SPEED_UPGRADE_RECIPE_SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(SimpleQuarries.QUARRY_BLOCK_ITEM));
        ingredients.add(Ingredient.of(SimpleQuarries.QUARRY_SPEED_UPGRADE_TEMPLATE));
        return ingredients;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }
}
