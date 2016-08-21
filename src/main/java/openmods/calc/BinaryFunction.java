package openmods.calc;

import openmods.utils.Stack;

public abstract class BinaryFunction<E> extends FixedCallable<E> {

	public BinaryFunction() {
		super(2, 1);
	}

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
