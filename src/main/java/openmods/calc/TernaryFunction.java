package openmods.calc;

import openmods.utils.Stack;

public abstract class TernaryFunction<E> extends FixedFunctionSymbol<E> {

	public TernaryFunction() {
		super(3, 1);
	}

	protected abstract E call(E first, E second, E third);

	@Override
	public final void call(ICalculatorFrame<E> frame) {
		final Stack<E> stack = frame.stack();

		final E first = stack.pop();
		final E second = stack.pop();
		final E third = stack.pop();
		final E result = call(first, second, third);
		stack.push(result);
	}

}
