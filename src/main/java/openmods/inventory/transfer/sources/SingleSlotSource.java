package openmods.inventory.transfer.sources;

import openmods.inventory.transfer.RevertableBase;
import net.minecraft.item.ItemStack;

public abstract class SingleSlotSource extends RevertableBase implements IItemStackSource {

	private int extracted;

	@Override
	public ItemStack pullAll() {
		ItemStack stack = getModifiableStack();
		if (stack == null || stack.stackSize <= 0) return null;

		final ItemStack original = getStack();
		if (!canExtract(original)) return null;

		extracted += stack.stackSize;

		setCachedStack(null);
		return stack;
	}

	@Override
	public ItemStack pull(int amount) {
		if (amount <= 0) return null;

		ItemStack stack = getModifiableStack();
		if (stack == null) return null;

		int used = Math.min(amount, stack.stackSize);
		if (used <= 0) return null;

		final int usableItems = stack.stackSize;

		stack.stackSize = extracted + used;
		boolean canExtract = canExtract(stack);
		stack.stackSize = usableItems;

		if (!canExtract) return null;

		extracted += used;
		ItemStack returned = stack.splitStack(used);
		setCachedStack(stack);
		return returned;
	}

	@Override
	public int totalExtracted() {
		return extracted;
	}

	@Override
	public boolean commit() {
		extracted = 0;
		return super.commit();
	}

	@Override
	public void abort() {
		extracted = 0;
		super.abort();
	}

	protected abstract boolean canExtract(ItemStack stack);

}
