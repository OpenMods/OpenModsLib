package openmods.calc;

import com.google.common.collect.Maps;
import java.util.Map;
import openmods.utils.Stack;

public class LocalFrame<E> implements ICalculatorFrame<E> {

	private final ICalculatorFrame<E> parent;

	private final Map<String, ISymbol<E>> locals = Maps.newHashMap();

	private final Stack<E> stack = new Stack<E>();

	public LocalFrame(ICalculatorFrame<E> parent) {
		this.parent = parent;
	}

	public void setLocalSymbol(String symbol, ISymbol<E> value) {
		locals.put(symbol, value);
	}

	@Override
	public ISymbol<E> getSymbol(String id) {
		final ISymbol<E> symbol = locals.get(id);
		return symbol != null? symbol : (parent != null? parent.getSymbol(id) : null);
	}

	@Override
	public Stack<E> stack() {
		return stack;
	}

}
