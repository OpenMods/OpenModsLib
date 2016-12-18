package openmods.calc;

import com.google.common.base.Preconditions;
import openmods.utils.Stack;

public abstract class NullaryFunction<E> extends FixedCallable<E> {

	public NullaryFunction() {
		super(0, 1);
	}

	public abstract static class Direct<E> extends NullaryFunction<E> {
		protected abstract E call();

		@Override
		public final void call(Frame<E> frame) {
			frame.stack().push(call());
		}
	}

	public abstract static class WithFrame<E> extends NullaryFunction<E> {
		protected abstract E callImpl(Frame<E> frame);

		@Override
		public final void call(Frame<E> frame) {
			final Frame<E> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 1);
			final Stack<E> stack = executionFrame.stack();

			final E result = callImpl(executionFrame);
			Preconditions.checkState(stack.isEmpty(), "Values left on stack");
			frame.stack().push(result);
		}
	}

	public static <E> ICallable<E> createConst(final E value) {
		return new NullaryFunction.Direct<E>() {
			@Override
			protected E call() {
				return value;
			}
		};
	}
}
