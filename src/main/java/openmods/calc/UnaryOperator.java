package openmods.calc;

import openmods.utils.Stack;

public abstract class UnaryOperator<E> extends Operator<E> {

	public UnaryOperator(int precendence, Associativity associativity) {
		super(precendence, associativity);
	}

	public UnaryOperator(int precendence) {
		super(precendence, Associativity.RIGHT);
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
