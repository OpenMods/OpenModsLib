package openmods.calc;

import openmods.utils.Stack;

public interface ICalculatorFrame<E> {
	public Stack<E> stack();

	public ISymbol<E> getSymbol(String id);
}
