package openmods.calc;

import openmods.utils.Stack;

public abstract class BinaryFunction<E> extends Function<E> {

	public BinaryFunction() {
		super(2, 1);
	}

	protected abstract E execute(E left, E right);

	@Override
	public final void execute(ICalculatorFrame<E> frame) {
		final Stack<E> stack = frame.stack();

		final E right = stack.pop();
		final E left = stack.pop();
		final E result = execute(left, right);
		stack.push(result);
	}

}
