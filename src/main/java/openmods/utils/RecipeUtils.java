package openmods.utils;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import openmods.Log;

public class RecipeUtils {

	private static final ItemStack[] EMPTY_ITEM_STACK_ARRAY = new ItemStack[0];

	private static Field shapedOreRecipeWidth;

	@SuppressWarnings("unchecked")
	public static List<IRecipe> getVanillaRecipes() {
		return CraftingManager.getInstance().getRecipeList();
	}

	public static ItemStack[] getFirstRecipeForItem(ItemStack resultingItem) {

		for (IRecipe recipe : getVanillaRecipes()) {
			if (recipe == null) continue;

			ItemStack result = recipe.getRecipeOutput();
			if (result == null || !result.isItemEqual(resultingItem)) continue;

			Object[] input = getRecipeInput(recipe);
			if (input != null) return convertToStacks(input);

		}
		return null;
	}

	public static ItemStack[] convertToStacks(Object[] input) {
		ItemStack[] result = new ItemStack[input.length];
		for (int i = 0; i < input.length; i++)
			result[i] = convertToStack(input[i]);

		return result;
	}

	public static ItemStack convertToStack(Object obj) {
		ItemStack entry = null;

		if (obj instanceof ItemStack) {
			entry = (ItemStack)obj;
		} else if (obj instanceof List) {
			@SuppressWarnings("unchecked")
			List<ItemStack> list = (List<ItemStack>)obj;
			entry = CollectionUtils.getRandom(list);
		}

		if (entry == null) return null;
		entry = entry.copy();
		if (entry.getItemDamage() == OreDictionary.WILDCARD_VALUE) entry.setItemDamage(0);
		return entry;
	}

	@SuppressWarnings("unchecked")
	public static Object[] getRecipeInput(IRecipe recipe) {
		if (recipe instanceof ShapelessOreRecipe) return ((ShapelessOreRecipe)recipe).getInput().toArray();
		else if (recipe instanceof ShapedOreRecipe) return getShapedOreRecipe((ShapedOreRecipe)recipe);
		else if (recipe instanceof ShapedRecipes) return ((ShapedRecipes)recipe).recipeItems;
		else if (recipe instanceof ShapelessRecipes) return ((ShapelessRecipes)recipe).recipeItems.toArray(EMPTY_ITEM_STACK_ARRAY);
		return null;
	}

	private static Object[] getShapedOreRecipe(ShapedOreRecipe recipe) {
		final int width;
		try {
			width = getRecipeWidth(recipe);
		} catch (Exception e) {
			Log.severe(e, "Failed to get input information from %s", recipe);
			return null;
		}

		Object[] input = recipe.getInput();
		int inputIndex = 0;
		Object[] grid = new Object[9];
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				final int outputIndex = y * 3 + x;
				if (x < width && inputIndex < input.length) {
					grid[outputIndex] = input[inputIndex];
					inputIndex++;
				} else {
					grid[outputIndex] = null;
				}
			}
		}
		return grid;
	}

	private static int getRecipeWidth(ShapedOreRecipe recipe) throws Exception {
		if (shapedOreRecipeWidth == null) {
			shapedOreRecipeWidth = ShapedOreRecipe.class.getDeclaredField("width");
			shapedOreRecipeWidth.setAccessible(true);
		}
		int width = shapedOreRecipeWidth.getInt(recipe);
		return width;
	}

}
