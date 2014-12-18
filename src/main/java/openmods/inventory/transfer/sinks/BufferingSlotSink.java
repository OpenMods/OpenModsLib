package openmods.inventory.transfer.sinks;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.google.common.collect.Lists;

public abstract class BufferingSlotSink implements IItemStackSink {

	private List<ItemStack> buffer = Lists.newArrayList();

	protected abstract void markDirty();

	protected abstract void pushStack(ItemStack stack);

	protected abstract boolean isValid(ItemStack stack);

	@Override
	public int accept(ItemStack stack) {
		if (stack == null) return 0;

		if (!isValid(stack)) return 0;

		buffer.add(stack.copy());
		return stack.stackSize;
	}

	@Override
	public boolean commit() {
		if (buffer.isEmpty()) return false;

		for (ItemStack item : buffer)
			pushStack(item);

		buffer.clear();
		markDirty();
		return true;
	}

	@Override
	public boolean hasChanges() {
		return !buffer.isEmpty();
	}

	@Override
	public void abort() {
		buffer.clear();
	}

}
