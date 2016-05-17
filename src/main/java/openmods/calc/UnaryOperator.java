package openmods.calc;

import openmods.utils.Stack;

public abstract class UnaryOperator<E> extends Operator<E> {

	public UnaryOperator(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	protected abstract E execute(E value);

	@Override
	public final void execute(ICalculatorFrame<E> frame) {
		final Stack<E> stack = frame.stack();

		final E value = stack.pop();
		final E result = execute(value);
		stack.push(result);
	}

	@Override
	public boolean isLessThan(Operator<E> other) {
		return false; // every other operator has lower or equal precendence
	}

	@Override
	public String toString() {
		return "UnaryOperator [" + id + "]";
	}

}
