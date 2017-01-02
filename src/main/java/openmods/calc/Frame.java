package openmods.calc;

import openmods.utils.Stack;

public class Frame<E> {
	private final SymbolMap<E> symbols;

	private final Stack<E> stack;

	public Frame(SymbolMap<E> symbols, Stack<E> stack) {
		this.symbols = symbols;
		this.stack = stack;
	}

	public Stack<E> stack() {
		return stack;
	}

	public SymbolMap<E> symbols() {
		return symbols;
	}
}
