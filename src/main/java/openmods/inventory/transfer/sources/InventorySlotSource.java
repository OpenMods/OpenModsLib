package openmods.inventory.transfer.sources;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class InventorySlotSource extends SingleSlotSource {

	private final IInventory inventory;

	private final int slot;

	public InventorySlotSource(IInventory inventory, int slot) {
		this.inventory = inventory;
		this.slot = slot;
	}

	@Override
	protected boolean canExtract(ItemStack stack) {
		return true;
	}

	@Override
	protected ItemStack getStack() {
		return inventory.getStackInSlot(slot);
	}

	@Override
	protected void markDirty() {
		inventory.markDirty();
	}

	@Override
	protected void setStack(ItemStack stack) {
		inventory.setInventorySlotContents(slot, stack);
	}

}
