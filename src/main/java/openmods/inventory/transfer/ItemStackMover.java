package openmods.inventory.transfer;

import net.minecraft.item.ItemStack;
import openmods.inventory.transfer.sinks.IItemStackSink;
import openmods.inventory.transfer.sources.IItemStackSource;

public class ItemStackMover {

	public enum Result {
		ALL_MOVED(true, false, false),
		PARTIAL_SINK(true, false, true),
		PARTIAL_SOURCE(true, true, false),
		PARTIAL_SINK_SOURCE(true, true, true),
		MOVE_FAILED(false, false, false);

		private Result(boolean itemsMoved, boolean partialSource, boolean partialSink) {
			this.itemsMoved = itemsMoved;
			this.partialSource = partialSource;
			this.partialSink = partialSink;
		}

		public final boolean itemsMoved;
		public final boolean partialSource;
		public final boolean partialSink;
	}

	private final int maxAmount;

	public ItemStackMover(int maxAmount) {
		this.maxAmount = maxAmount;
	}

	public Result move(IItemStackSource source, IItemStackSink sink) {
		ItemStack stack = source.pull(maxAmount);
		if (stack == null || stack.stackSize == 0) return Result.PARTIAL_SOURCE;
		final boolean partialSource = stack.stackSize != maxAmount;

		final int used = sink.accept(stack);
		if (used == 0) {
			source.abort();
			return Result.PARTIAL_SOURCE;
		}

		final boolean partialSink = used != stack.stackSize;

		if (partialSink) {
			source.abort();

			ItemStack partialStack = source.pull(used);
			if (partialStack == null || partialStack.stackSize != used) {
				source.abort();
				sink.abort();
				// I don't do bussiness with crazies
				return Result.MOVE_FAILED;
			}

			source.commit();
			sink.commit();
		}

		source.commit();
		sink.commit();

		if (partialSource && partialSink) return Result.PARTIAL_SINK_SOURCE;
		else if (partialSink) return Result.PARTIAL_SINK;
		else if (partialSource) return Result.PARTIAL_SOURCE;
		else return Result.ALL_MOVED;
	}

}
