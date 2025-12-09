package com.simplequarries.recipe;

import com.simplequarries.QuarryUpgrades;
import com.simplequarries.SimpleQuarries;
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

public class QuarryUpgradeRecipe extends SpecialCraftingRecipe {
    public QuarryUpgradeRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        ItemStack quarryStack = ItemStack.EMPTY;
        int templateCount = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.isOf(SimpleQuarries.QUARRY_BLOCK_ITEM)) {
                if (!quarryStack.isEmpty()) {
                    return false; // Multiple quarries present
                }
                quarryStack = stack;
            } else if (stack.isOf(SimpleQuarries.QUARRY_UPGRADE_TEMPLATE)) {
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

        int currentUpgrades = QuarryBlockItem.getUpgradeCount(quarryStack);
        return currentUpgrades < QuarryUpgrades.MAX_UPGRADES;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup registries) {
        ItemStack quarryStack = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.isOf(SimpleQuarries.QUARRY_BLOCK_ITEM)) {
                quarryStack = stack;
                break;
            }
        }

        if (quarryStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = quarryStack.copy();
        result.setCount(1);
        int upgradedCount = QuarryBlockItem.getUpgradeCount(quarryStack) + 1;
        QuarryBlockItem.setUpgradeCount(result, upgradedCount);
        return result;
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return SimpleQuarries.QUARRY_UPGRADE_RECIPE_SERIALIZER;
    }

    public DefaultedList<net.minecraft.recipe.Ingredient> getIngredients() {
        DefaultedList<Ingredient> ingredients = DefaultedList.of();
        ingredients.add(Ingredient.ofItems(SimpleQuarries.QUARRY_BLOCK_ITEM));
        ingredients.add(Ingredient.ofItems(SimpleQuarries.QUARRY_UPGRADE_TEMPLATE));
        return ingredients;
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
        return DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY);
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }
}
