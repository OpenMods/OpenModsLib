package openmods.utils;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.ForgeHooks;

public abstract class CustomRecipeBase implements IRecipe {

	@Override
	public ItemStack getRecipeOutput() {
		return null;
	}

	@Override
	public ItemStack[] getRemainingItems(InventoryCrafting inv) {
		ItemStack[] result = new ItemStack[inv.getSizeInventory()];

		for (int i = 0; i < result.length; ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			result[i] = ForgeHooks.getContainerItem(itemstack);
		}

		return result;
	}

}
