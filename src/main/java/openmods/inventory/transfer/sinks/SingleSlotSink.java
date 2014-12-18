package openmods.inventory.transfer.sinks;

import net.minecraft.item.ItemStack;
import openmods.inventory.InventoryUtils;
import openmods.inventory.transfer.RevertableBase;

public abstract class SingleSlotSink extends RevertableBase implements IItemStackSink {

	@Override
	public int accept(ItemStack incoming) {
		if (incoming == null || incoming.stackSize <= 0) return 0;
		final ItemStack inSlot = getCachedStack();

		if (inSlot == null || inSlot.stackSize == 0) {
			if (!isValid(incoming)) return 0;
			final int amount = Math.min(incoming.stackSize, getSlotLimit());
			if (amount > 0) {
				ItemStack overrideStack = incoming.copy();
				overrideStack.stackSize = amount;
				setCachedStack(overrideStack);
			}
			return amount;
		} else if (InventoryUtils.STACK_EQUALITY_TESTER.isEqual(inSlot, incoming)) {
			if (!isValid(incoming)) return 0; // weird case, but why not?
			final int limit = Math.min(inSlot.getMaxStackSize(), getSlotLimit());
			final int space = limit - inSlot.stackSize;

			final int used = Math.min(space, incoming.stackSize);
			if (used > 0) {
				ItemStack changedStack = getModifiableStack();
				changedStack.stackSize += used;
				setCachedStack(changedStack);
			}
			return used;
		}

		return 0;
	}

	protected abstract boolean isValid(ItemStack stack);

	protected abstract int getSlotLimit();

}
