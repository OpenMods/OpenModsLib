package openmods.inventory.transfer.sources;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

public class SidedInventorySource extends SingleSlotSource {

	private final ISidedInventory inventory;

	private final int slot;

	private final int side;

	public SidedInventorySource(ISidedInventory inventory, int slot, ForgeDirection side) {
		this.inventory = inventory;
		this.slot = slot;
		this.side = side.ordinal();
	}

	@Override
	protected boolean canExtract(ItemStack stack) {
		return inventory.canExtractItem(slot, stack, side);
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
