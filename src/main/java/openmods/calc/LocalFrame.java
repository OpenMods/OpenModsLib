package openmods.calc;

import openmods.utils.Stack;

public class LocalFrame<E> extends NestedFrame<E> {

	private final Stack<E> stack = new Stack<E>();

	public LocalFrame(ICalculatorFrame<E> parent) {
		super(parent);
	}

	@Override
	public Stack<E> stack() {
		return stack;
	}

}
