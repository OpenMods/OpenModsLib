package openmods.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class FakeSlot extends Slot implements ICustomSlot {

	private final boolean keepSize;

	public FakeSlot(IInventory inventory, int slot, int x, int y, boolean keepSize) {
		super(inventory, slot, x, y);
		this.keepSize = keepSize;
	}

	@Override
	public ItemStack onClick(EntityPlayer player, int dragType, ClickType clickType) {
		if (clickType == ClickType.CLONE && player.capabilities.isCreativeMode) { // TODO 1.11 verify
			ItemStack contents = getStack();
			if (!contents.isEmpty()) {
				ItemStack tmp = contents.copy();
				tmp.setCount(tmp.getMaxStackSize());
				player.inventory.setItemStack(tmp);
				return tmp;
			}
		}

		ItemStack held = player.inventory.getItemStack();

		ItemStack place = ItemStack.EMPTY;

		if (!held.isEmpty()) {
			place = held.copy();
			if (!keepSize) place.setCount(1);
		}

		inventory.setInventorySlotContents(slotNumber, place);
		onSlotChanged();
		return place;
	}

	@Override
	public boolean canDrag() {
		return false;
	}

	@Override
	public boolean canTransferItemsOut() {
		return false;
	}

	@Override
	public boolean canTransferItemsIn() {
		return false;
	}

}
