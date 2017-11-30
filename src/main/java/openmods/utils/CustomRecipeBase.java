package openmods.utils;

import javax.annotation.Nonnull;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class CustomRecipeBase extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

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
	public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
		NonNullList<ItemStack> result = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

		for (int i = 0; i < result.size(); ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			result.set(i, ForgeHooks.getContainerItem(itemstack));
		}

		return result;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

}
