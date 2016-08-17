package openmods.calc;

import openmods.utils.Stack;

public class SubFrame<E> extends NestedFrame<E> {

	private final Stack<E> stack;

	public SubFrame(ICalculatorFrame<E> parent, int args) {
		super(parent);
		this.stack = parent.stack().substack(args);
	}

	@Override
	public Stack<E> stack() {
		return stack;
	}

}
