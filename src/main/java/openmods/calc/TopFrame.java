package openmods.calc;

import com.google.common.collect.Maps;
import java.util.Map;
import openmods.utils.Stack;

public class TopFrame<E> implements ICalculatorFrame<E> {

	private final Stack<E> stack;

	private final Map<String, ISymbol<E>> globals;

	protected TopFrame(Stack<E> stack, Map<String, ISymbol<E>> globals) {
		this.stack = stack;
		this.globals = globals;
	}

	public TopFrame() {
		this(Stack.<E> create(), Maps.<String, ISymbol<E>> newHashMap());
	}

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

	public TopFrame<E> cloneWithSymbols() {
		return new TopFrame<E>(Stack.<E> create(), Maps.newHashMap(globals));
	}
}
