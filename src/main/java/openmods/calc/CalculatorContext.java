package openmods.calc;

import openmods.utils.Stack;

public class CalculatorContext<E> {

	public final Stack<E> stack;

	public final SymbolDictionary<E> symbols;

	public CalculatorContext(Stack<E> stack, SymbolDictionary<E> symbols) {
		this.stack = stack;
		this.symbols = symbols;
	}

}
