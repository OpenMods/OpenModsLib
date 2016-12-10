package openmods.calc;

import openmods.utils.Stack;

public abstract class UnaryFunction<E> extends FixedCallable<E> {

	public UnaryFunction() {
		super(1, 1);
	}

	protected abstract E call(E value);

	@Override
	public final void call(Frame<E> frame) {
		final Stack<E> stack = frame.stack();

		final E value = stack.pop();
		final E result = call(value);
		stack.push(result);
	}

	public static <E> ICallable<E> createConst(final E value) {
		return new UnaryFunction<E>() {
			@Override
			protected E call(E value) {
				return value;
			}
		};
	}

}
