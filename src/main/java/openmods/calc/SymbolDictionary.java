package openmods.calc;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public abstract class SymbolDictionary<E> {

	private final SymbolDictionary<E> parent;

	private final Map<String, IExecutable<E>> symbols = Maps.newHashMap();

	public SymbolDictionary(SymbolDictionary<E> parent) {
		this.parent = parent;
	}

	private void registerSymbol(String symbol, IExecutable<E> executable) {
		final IExecutable<E> prev = symbols.put(symbol, executable);
		Preconditions.checkState(prev == null, "Duplicate symbol %s: %s -> %s", prev, executable);
	}

	public void registerConstant(String symbol, E value) {
		registerSymbol(symbol, Constant.create(value));
	}

	public Variable<E> registerVariable(String symbol, E initialValue) {
		final Variable<E> result = Variable.<E> create(initialValue);
		registerSymbol(symbol, result);
		return result;
	}

	public void registerFunction(String symbol, IFunction<E> function) {
		registerSymbol(symbol, function);
	}

	public Map<String, IExecutable<E>> getSymbolsMap() {
		return ImmutableMap.copyOf(symbols);
	}

	public IExecutable<E> get(String token) {
		IExecutable<E> symbol = symbols.get(token);
		if (symbol != null) return symbol;

		if (parent != null) return parent.get(token);

		return null;
	}
}
