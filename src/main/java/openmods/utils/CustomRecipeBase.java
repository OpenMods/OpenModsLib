package openmods.utils;

import javax.annotation.Nonnull;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

public abstract class CustomRecipeBase<T extends IInventory> implements IRecipe<T> {

	private final String group;

	public CustomRecipeBase(String group) {
		this.group = group;
	}

	public CustomRecipeBase(ResourceLocation group) {
		this.group = group.toString();
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	@Nonnull
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

}
