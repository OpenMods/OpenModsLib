package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;

public abstract class NestedFrame<E> implements ICalculatorFrame<E> {

	private final ICalculatorFrame<E> parent;

	private final Map<String, ISymbol<E>> locals = Maps.newHashMap();

	public NestedFrame(ICalculatorFrame<E> parent) {
		Preconditions.checkNotNull(parent);
		this.parent = parent;
	}

	public void setLocalSymbol(String symbol, ISymbol<E> value) {
		locals.put(symbol, value);
	}

	@Override
	public ISymbol<E> getSymbol(String id) {
		final ISymbol<E> symbol = locals.get(id);
		return symbol != null? symbol : parent.getSymbol(id);
	}
}
