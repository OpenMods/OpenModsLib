package openmods.utils;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public class RecipeUtils {
	public static IRecipe getFirstRecipeForItemStack(World world, @Nonnull ItemStack resultingItem) {
		return getFirstRecipeForItemStack(world.getRecipeManager(), resultingItem);
	}

	public static IRecipe getFirstRecipeForItemStack(RecipeManager recipeManager, @Nonnull ItemStack resultingItem) {
		for (IRecipe recipe : recipeManager.getRecipes()) {
			if (recipe == null) continue;

			ItemStack result = recipe.getRecipeOutput();
			if (!result.isEmpty() && result.isItemEqual(resultingItem)) return recipe;

		}
		return null;
	}

	public static ItemStack[][] getFullRecipeInput(IRecipe<?> recipe) {
		final NonNullList<Ingredient> ingredients = recipe.getIngredients();
		final int ingredientCount = ingredients.size();
		final ItemStack[][] result = new ItemStack[ingredientCount][];

		for (int i = 0; i < ingredientCount; i++)
			result[i] = ingredients.get(i).getMatchingStacks();

		return result;
	}

}
