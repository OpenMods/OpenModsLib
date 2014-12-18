package openmods.inventory.transfer.sinks;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.google.common.base.Preconditions;

public class SingleInventorySlotSink extends SingleSlotSink {

	private final int slot;

	private final IInventory inventory;

	public SingleInventorySlotSink(IInventory inventory, int slot) {
		Preconditions.checkNotNull(inventory);
		this.inventory = inventory;
		this.slot = slot;
	}

	@Override
	protected int getSlotLimit() {
		return inventory.getInventoryStackLimit();
	}

	@Override
	protected ItemStack getStack() {
		return inventory.getStackInSlot(slot);
	}

	@Override
	protected boolean isValid(ItemStack stack) {
		return inventory.isItemValidForSlot(slot, stack);
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
