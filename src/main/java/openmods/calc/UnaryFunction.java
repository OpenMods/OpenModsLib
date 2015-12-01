package openmods.calc;

import openmods.utils.Stack;

public abstract class UnaryFunction<E> extends FixedSymbol<E> {

	public UnaryFunction() {
		super(1, 1);
	}

	protected abstract E execute(E value);

	@Override
	public final void execute(ICalculatorFrame<E> frame) {
		final Stack<E> stack = frame.stack();

		final E value = stack.pop();
		final E result = execute(value);
		stack.push(result);
	}

}
