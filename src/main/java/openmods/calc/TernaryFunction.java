package openmods.calc;

import com.google.common.base.Preconditions;
import openmods.utils.Stack;

public abstract class TernaryFunction<E> extends FixedCallable<E> {

	private TernaryFunction() {
		super(3, 1);
	}

	public abstract static class Direct<E> extends TernaryFunction<E> {
		protected abstract E call(E first, E second, E third);

		@Override
		public final void call(Frame<E> frame) {
			final Stack<E> stack = frame.stack();

			final E third = stack.pop();
			final E second = stack.pop();
			final E first = stack.pop();

			final E result = call(first, second, third);
			stack.push(result);
		}
	}

	public abstract static class WithFrame<E> extends TernaryFunction<E> {
		protected abstract E call(Frame<E> frame, E first, E second, E third);

		@Override
		public final void call(Frame<E> frame) {
			final Frame<E> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 3);
			final Stack<E> stack = executionFrame.stack();

			final E third = stack.pop();
			final E second = stack.pop();
			final E first = stack.pop();

			final E result = call(executionFrame, first, second, third);
			Preconditions.checkState(stack.isEmpty(), "Values left on stack");
			stack.push(result);
		}
	}
}
