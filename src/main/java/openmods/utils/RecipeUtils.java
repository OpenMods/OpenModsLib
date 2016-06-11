package openmods.utils;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import openmods.reflection.FieldAccess;

public class RecipeUtils {

	public static class InputBuilder {
		private static final ItemStack[] EMPTY_SLOT = new ItemStack[0];

		private static final Function<ItemStack, ItemStack> COPY_TRANSFORM = new Function<ItemStack, ItemStack>() {
			@Override
			@Nullable
			public ItemStack apply(@Nullable ItemStack input) {
				return input != null? input.copy() : null;
			}
		};

		private final ItemStack[][] slots;

		public InputBuilder(int size) {
			slots = new ItemStack[size][];
		}

		public void add(int slot, ItemStack stack) {
			slots[slot] = stack == null? EMPTY_SLOT : new ItemStack[] { stack.copy() };
		}

		public void add(int slot, ItemStack[] stacks) {
			slots[slot] = CollectionUtils.transform(stacks, COPY_TRANSFORM);
		}

		public void add(int slot, Collection<ItemStack> stacks) {
			slots[slot] = CollectionUtils.transform(stacks, COPY_TRANSFORM);
		}

		public ItemStack[][] build() {
			for (int i = 0; i < slots.length; i++)
				if (slots[i] == null) slots[i] = EMPTY_SLOT;

			return slots;
		}
	}

	private static final ItemStack[] EMPTY_ITEM_STACK_ARRAY = new ItemStack[0];

	private static FieldAccess<Integer> shapedOreRecipeWidth = FieldAccess.create(ShapedOreRecipe.class, "width");

	private static FieldAccess<Integer> shapedOreRecipeHeight = FieldAccess.create(ShapedOreRecipe.class, "height");

	@SuppressWarnings("unchecked")
	public static List<IRecipe> getVanillaRecipes() {
		return CraftingManager.getInstance().getRecipeList();
	}

	public static IRecipe getFirstRecipeForItemStack(ItemStack resultingItem) {

		for (IRecipe recipe : getVanillaRecipes()) {
			if (recipe == null) continue;

			ItemStack result = recipe.getRecipeOutput();
			if (result != null && result.isItemEqual(resultingItem)) return recipe;

		}
		return null;
	}

	public static ItemStack[] getFirstRecipeForItem(ItemStack resultingItem) {
		final IRecipe recipe = getFirstRecipeForItemStack(resultingItem);
		if (recipe == null) return null;

		Object[] input = getRecipeInput(recipe);
		return input != null? convertToStacks(input) : null;
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
		final int width = shapedOreRecipeWidth.get(recipe);

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

	@SuppressWarnings("unchecked")
	private static void addOreRecipeEntry(InputBuilder builder, int slot, Object value) {
		if (value instanceof ItemStack) builder.add(slot, (ItemStack)value);
		else if (value instanceof Collection) {
			List<ItemStack> variants = Lists.newArrayList();
			for (ItemStack stack : (Collection<ItemStack>)value) {
				if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
					final Item item = stack.getItem();
					item.getSubItems(item, null, variants);
				} else {
					variants.add(stack);
				}
			}

			builder.add(slot, variants);
		}
	}

	public static ItemStack[][] getFullRecipeInput(IRecipe recipe) {
		if (recipe instanceof ShapelessOreRecipe) return getFullRecipeInput(((ShapelessOreRecipe)recipe));
		else if (recipe instanceof ShapedOreRecipe) return getFullRecipeInput((ShapedOreRecipe)recipe);
		else if (recipe instanceof ShapedRecipes) return getFullRecipeInput((ShapedRecipes)recipe);
		else if (recipe instanceof ShapelessRecipes) return getFullRecipeInput((ShapelessRecipes)recipe);
		return null;
	}

	public static ItemStack[][] getFullRecipeInput(ShapelessOreRecipe recipe) {
		final List<Object> inputs = recipe.getInput();

		final int size = inputs.size();
		InputBuilder builder = new InputBuilder(size);

		for (int i = 0; i < size; i++) {
			final Object input = inputs.get(i);
			addOreRecipeEntry(builder, i, input);
		}

		return builder.build();
	}

	public static ItemStack[][] getFullRecipeInput(ShapedOreRecipe recipe) {
		final InputBuilder builder = new InputBuilder(9);
		final int width = shapedOreRecipeWidth.get(recipe);
		final int height = shapedOreRecipeHeight.get(recipe);

		final Object[] input = recipe.getInput();
		int inputIndex = 0;

		for (int row = 0; row < height; row++) {
			for (int column = 0; column < width; column++) {
				final int outputIndex = row * 3 + column;
				addOreRecipeEntry(builder, outputIndex, input[inputIndex]);
				inputIndex++;

			}
		}

		return builder.build();
	}

	public static ItemStack[][] getFullRecipeInput(ShapedRecipes recipe) {
		final InputBuilder builder = new InputBuilder(9);

		final ItemStack[] input = recipe.recipeItems;
		int inputIndex = 0;

		for (int row = 0; row < recipe.recipeHeight; row++) {
			for (int column = 0; column < recipe.recipeWidth; column++) {
				final int outputIndex = row * 3 + column;
				builder.add(outputIndex, input[inputIndex]);
				inputIndex++;
			}
		}

		return builder.build();
	}

	public static ItemStack[][] getFullRecipeInput(ShapelessRecipes recipe) {
		final InputBuilder builder = new InputBuilder(9);
		@SuppressWarnings("unchecked")
		final List<ItemStack> input = recipe.recipeItems;

		for (int i = 0; i < recipe.getRecipeSize(); i++)
			builder.add(i, input.get(i));

		return builder.build();
	}

}
