package openmods.calc;

import openmods.utils.Stack;

public abstract class UnaryFunction<E> extends FixedCallable<E> {

	private UnaryFunction() {
		super(1, 1);
	}

	public abstract static class Direct<E> extends UnaryFunction<E> {
		protected abstract E call(E value);

		@Override
		public final void call(Frame<E> frame) {
			final Stack<E> stack = frame.stack();

			final E value = stack.pop();
			final E result = call(value);
			stack.push(result);
		}
	}

	public abstract static class WithFrame<E> extends UnaryFunction<E> {
		protected abstract E call(Frame<E> frame, E value);

		@Override
		public final void call(Frame<E> frame) {
			final Frame<E> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 1);
			final Stack<E> stack = executionFrame.stack();

			final E value = stack.pop();
			final E result = call(executionFrame, value);
			stack.checkIsEmpty().push(result);
		}
	}

	public static <E> ICallable<E> createConst(final E value) {
		return new UnaryFunction.Direct<E>() {
			@Override
			protected E call(E value) {
				return value;
			}
		};
	}

}
