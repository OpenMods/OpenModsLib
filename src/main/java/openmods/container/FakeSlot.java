package openmods.container;

import net.minecraft.entity.player.EntityPlayer;
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
	public ItemStack onClick(EntityPlayer player, int button, int modifier) {
		if (button == 2 && player.capabilities.isCreativeMode) {
			ItemStack contents = getStack();
			if (contents != null) {
				ItemStack tmp = contents.copy();
				tmp.stackSize = tmp.getMaxStackSize();
				player.inventory.setItemStack(tmp);
				return tmp;
			}
		}

		ItemStack held = player.inventory.getItemStack();

		ItemStack place = null;

		if (held != null) {
			place = held.copy();
			if (!keepSize) place.stackSize = 1;
		}

		inventory.setInventorySlotContents(slotNumber, place);
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
