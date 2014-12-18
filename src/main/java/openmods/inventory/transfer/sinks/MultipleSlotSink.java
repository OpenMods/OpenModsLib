package openmods.inventory.transfer.sinks;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.google.common.collect.ImmutableList;

public abstract class MultipleSlotSink implements IItemStackSink {

	private final List<IItemStackSink> sinks;

	public MultipleSlotSink(Iterable<IItemStackSink> sinks) {
		this.sinks = ImmutableList.copyOf(sinks);
	}

	protected abstract void markDirty();

	@Override
	public int accept(ItemStack stack) {
		if (stack == null || stack.stackSize <= 0) return 0;

		final int initalAmount = stack.stackSize;
		int totalUsed = 0;

		for (IItemStackSink sink : sinks) {
			final int used = sink.accept(stack);

			if (used > 0) {
				totalUsed += used;
				stack.stackSize -= used;
				if (stack.stackSize <= 0) break;
			}
		}

		stack.stackSize = initalAmount;
		return totalUsed;
	}

	@Override
	public void abort() {
		for (IItemStackSink sink : sinks)
			sink.abort();
	}

	@Override
	public boolean commit() {
		boolean changed = false;

		for (IItemStackSink sink : sinks)
			changed |= sink.commit();

		if (changed) markDirty();

		return changed;
	}

	@Override
	public boolean hasChanges() {
		boolean changed = false;

		for (IItemStackSink sink : sinks)
			changed |= sink.hasChanges();

		return changed;
	}

}
