package openmods.inventory;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public interface ISidedInventoryDelegate extends IInventoryDelegate, ISidedInventory, ISidedInventoryProvider {
	@Override
	default int[] getSlotsForFace(EnumFacing side) {
		return getInventory().getSlotsForFace(side);
	}

	@Override
	default boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
		return getInventory().canInsertItem(index, itemStackIn, direction);
	}

	@Override
	default boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
		return getInventory().canExtractItem(index, stack, direction);
	}
}
