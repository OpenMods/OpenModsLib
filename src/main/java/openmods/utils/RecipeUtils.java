package openmods.utils;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import openmods.reflection.FieldAccess;

public class RecipeUtils {

	public static class InputBuilder {
		private static final ItemStack[] EMPTY_SLOT = new ItemStack[0];

		private final Map<Integer, NonNullList<ItemStack>> slots = Maps.newHashMap();

		public InputBuilder() {}

		private NonNullList<ItemStack> getSlot(int slot) {
			NonNullList<ItemStack> result = slots.get(slot);
			if (result == null) {
				result = NonNullList.create();
				slots.put(slot, result);
			}
			return result;
		}

		private static void addItemStack(final NonNullList<ItemStack> slot, ItemStack stack) {
			if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
				final Item item = stack.getItem();
				item.getSubItems(item, null, slot);
			} else {
				slot.add(stack.copy());
			}
		}

		public void add(int slot, ItemStack stack) {
			if (!stack.isEmpty()) {
				final NonNullList<ItemStack> slotContents = getSlot(slot);
				addItemStack(slotContents, stack);
			}
		}

		public void add(int slot, ItemStack[] stacks) {
			final NonNullList<ItemStack> slotContents = getSlot(slot);
			for (ItemStack stack : stacks) {
				if (!stack.isEmpty())
					addItemStack(slotContents, stack);
			}
		}

		public void add(int slot, Collection<ItemStack> stacks) {
			final NonNullList<ItemStack> slotContents = getSlot(slot);
			for (ItemStack stack : stacks) {
				if (stack != null)
					addItemStack(slotContents, stack);
			}
		}

		public ItemStack[][] build(int slotCount) {
			ItemStack[][] result = new ItemStack[slotCount][];
			for (int i = 0; i < slotCount; i++) {
				final List<ItemStack> slot = slots.get(i);
				if (slot != null) {
					result[i] = slot.toArray(new ItemStack[slot.size()]);
				} else {
					result[i] = EMPTY_SLOT;
				}
			}

			return result;
		}
	}

	private static FieldAccess<Integer> shapedOreRecipeWidth = FieldAccess.create(ShapedOreRecipe.class, "width");

	private static FieldAccess<Integer> shapedOreRecipeHeight = FieldAccess.create(ShapedOreRecipe.class, "height");

	public static List<IRecipe> getVanillaRecipes() {
		return CraftingManager.getInstance().getRecipeList();
	}

	public static IRecipe getFirstRecipeForItemStack(@Nonnull ItemStack resultingItem) {

		for (IRecipe recipe : getVanillaRecipes()) {
			if (recipe == null) continue;

			ItemStack result = recipe.getRecipeOutput();
			if (!result.isEmpty() && result.isItemEqual(resultingItem)) return recipe;

		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static void addOreRecipeEntry(InputBuilder builder, int slot, Object value) {
		if (value instanceof ItemStack) builder.add(slot, (ItemStack)value);
		else if (value instanceof Collection) builder.add(slot, (Collection<ItemStack>)value);
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
		InputBuilder builder = new InputBuilder();

		for (int i = 0; i < size; i++) {
			final Object input = inputs.get(i);
			addOreRecipeEntry(builder, i, input);
		}

		return builder.build(size);
	}

	public static ItemStack[][] getFullRecipeInput(ShapedOreRecipe recipe) {
		final InputBuilder builder = new InputBuilder();
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

		return builder.build(9);
	}

	public static ItemStack[][] getFullRecipeInput(ShapedRecipes recipe) {
		final InputBuilder builder = new InputBuilder();

		final ItemStack[] input = recipe.recipeItems;
		int inputIndex = 0;

		for (int row = 0; row < recipe.recipeHeight; row++) {
			for (int column = 0; column < recipe.recipeWidth; column++) {
				final int outputIndex = row * 3 + column;
				builder.add(outputIndex, input[inputIndex]);
				inputIndex++;
			}
		}

		return builder.build(9);
	}

	public static ItemStack[][] getFullRecipeInput(ShapelessRecipes recipe) {
		final InputBuilder builder = new InputBuilder();
		final List<ItemStack> input = recipe.recipeItems;

		final int size = recipe.getRecipeSize();
		for (int i = 0; i < size; i++)
			builder.add(i, input.get(i));

		return builder.build(size);
	}

}
