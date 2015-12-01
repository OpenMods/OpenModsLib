package openmods.calc;

import openmods.utils.Stack;

public abstract class TernaryFunction<E> extends FixedSymbol<E> {

	public TernaryFunction() {
		super(3, 1);
	}

	protected abstract E execute(E first, E second, E third);

	@Override
	public final void execute(ICalculatorFrame<E> frame) {
		final Stack<E> stack = frame.stack();

		final E first = stack.pop();
		final E second = stack.pop();
		final E third = stack.pop();
		final E result = execute(first, second, third);
		stack.push(result);
	}

}
