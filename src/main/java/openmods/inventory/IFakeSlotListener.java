package openmods.inventory;

import net.minecraft.item.ItemStack;

public interface IFakeSlotListener {
	public void onFakeSlotChange(int slot, ItemStack stack);
}
