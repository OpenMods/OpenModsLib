package openmods.calc;

import com.google.common.base.Preconditions;
import openmods.utils.Stack;

public abstract class BinaryFunction<E> extends FixedCallable<E> {

	private BinaryFunction() {
		super(2, 1);
	}

	public abstract static class Direct<E> extends BinaryFunction<E> {

		protected abstract E call(E left, E right);

		@Override
		public final void call(Frame<E> frame) {
			final Stack<E> stack = frame.stack();

			final E right = stack.pop();
			final E left = stack.pop();
			final E result = call(left, right);
			stack.push(result);
		}
	}

	public abstract static class WithFrame<E> extends BinaryFunction<E> {

		protected abstract E call(Frame<E> frame, E left, E right);

		@Override
		public final void call(Frame<E> frame) {
			final Frame<E> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 2);
			final Stack<E> stack = executionFrame.stack();

			final E right = stack.pop();
			final E left = stack.pop();
			final E result = call(executionFrame, left, right);
			Preconditions.checkState(stack.isEmpty(), "Values left on stack");
			stack.push(result);
		}
	}

}
