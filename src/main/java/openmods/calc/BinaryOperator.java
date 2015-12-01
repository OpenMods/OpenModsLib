package openmods.calc;

import openmods.utils.Stack;

public abstract class BinaryOperator<E> extends Operator<E> {

	public BinaryOperator(int precendence, Associativity associativity) {
		super(precendence, associativity);
	}

	public BinaryOperator(int precendence) {
		super(precendence, Associativity.LEFT);
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
