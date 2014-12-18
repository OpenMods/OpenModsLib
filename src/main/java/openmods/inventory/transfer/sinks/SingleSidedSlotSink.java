package openmods.inventory.transfer.sinks;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

public class SingleSidedSlotSink extends SingleSlotSink {

	private final ISidedInventory inventory;

	private final int slot;

	private final int side;

	public SingleSidedSlotSink(ISidedInventory inventory, int slot, ForgeDirection side) {
		this.inventory = inventory;
		this.slot = slot;
		this.side = side.ordinal();
	}

	@Override
	protected boolean isValid(ItemStack stack) {
		return inventory.isItemValidForSlot(slot, stack) &&
				inventory.canInsertItem(slot, stack, side);
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
	protected void markDirty() {
		inventory.markDirty();
	}

	@Override
	protected void setStack(ItemStack stack) {
		inventory.setInventorySlotContents(slot, stack);
	}

}
