package openmods.calc;

import java.util.Map;

import openmods.utils.Stack;

import com.google.common.collect.Maps;

public class TopFrame<E> implements ICalculatorFrame<E> {

	private final Stack<E> stack = new Stack<E>();

	private final Map<String, ISymbol<E>> globals = Maps.newHashMap();

	public void setSymbol(String id, ISymbol<E> value) {
		globals.put(id, value);
	}

	@Override
	public ISymbol<E> getSymbol(String id) {
		return globals.get(id);
	}

	@Override
	public Stack<E> stack() {
		return stack;
	}
}
