package openmods.utils;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

public class RecipeUtils {

	public static IRecipe getFirstRecipeForItemStack(@Nonnull ItemStack resultingItem) {

		for (IRecipe recipe : CraftingManager.REGISTRY) {
			if (recipe == null) continue;

			ItemStack result = recipe.getRecipeOutput();
			if (!result.isEmpty() && result.isItemEqual(resultingItem)) return recipe;

		}
		return null;
	}

	public static ItemStack[][] getFullRecipeInput(IRecipe recipe) {
		final NonNullList<Ingredient> ingredients = recipe.getIngredients();
		final int ingredientCount = ingredients.size();
		final ItemStack[][] result = new ItemStack[ingredientCount][];

		for (int i = 0; i < ingredientCount; i++)
			result[i] = ingredients.get(i).getMatchingStacks();

		return result;
	}

}
