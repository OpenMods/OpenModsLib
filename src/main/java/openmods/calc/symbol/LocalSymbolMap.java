package openmods.calc.symbol;

import com.google.common.collect.Maps;
import java.util.Map;

public class LocalSymbolMap<E> extends NestedSymbolMap<E> {

	private final Map<String, ISymbol<E>> locals = Maps.newHashMap();

	public LocalSymbolMap(SymbolMap<E> parent) {
		super(parent);
	}

	@Override
	public void put(String name, ISymbol<E> symbol) {
		locals.put(name, symbol);
	}

	@Override
	public ISymbol<E> get(String name) {
		final ISymbol<E> symbol = locals.get(name);
		return symbol != null? symbol : super.get(name);
	}

}
