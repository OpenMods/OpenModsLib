package openmods.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

// Fake slot - sometimes also called "Phantom" or "Ghost" slot. It does not
// allow any items being taken out or placed in. Changing items in the
// inventory is done manually to prevent actually using up an item etc. e.g.
// BuildCraft uses the same system.
public class FakeSlot extends Slot {
	public FakeSlot(IInventory inventory, int slotIndex, int x, int y) {
		super(inventory, slotIndex, x, y);
	}

	@Override
	public boolean canTakeStack(EntityPlayer player) {
		// Nope!
		return false;
	}

	@Override
	public boolean isItemValid(ItemStack stack) {
		// Nope!
		return false;
	}

	@Override
	public int getSlotStackLimit() {
		// This is not really relevant, but the reality is only 1 item per
		// stack is ever allowed. We can be nice and communicate that.
		return 1;
	}
}
