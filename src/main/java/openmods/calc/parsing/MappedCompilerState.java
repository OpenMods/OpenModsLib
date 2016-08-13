package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;

public class MappedCompilerState<E> implements ICompilerState<E> {

	private final IAstParser<E> parser;

	private final Map<String, ISymbolCallStateTransition<E>> symbolTransitions = Maps.newHashMap();

	private final Map<String, IModifierStateTransition<E>> modifierTransitions = Maps.newHashMap();

	public MappedCompilerState(IAstParser<E> parser) {
		this.parser = parser;
	}

	@Override
	public IAstParser<E> getParser() {
		return parser;
	}

	@Override
	public ISymbolCallStateTransition<E> getStateForSymbolCall(String symbol) {
		final ISymbolCallStateTransition<E> stateTransition = symbolTransitions.get(symbol);
		return stateTransition != null? stateTransition : createDefaultSymbolCallStateTransition(symbol);
	}

	protected ISymbolCallStateTransition<E> createDefaultSymbolCallStateTransition(final String symbol) {
		return new ISymbolCallStateTransition<E>() {
			@Override
			public ICompilerState<E> getState() {
				return MappedCompilerState.this;
			}

			@Override
			public IExprNode<E> createRootNode(List<IExprNode<E>> children) {
				return new SymbolCallNode<E>(symbol, children);
			}
		};
	}

	@Override
	public IModifierStateTransition<E> getStateForModifier(String modifier) {
		final IModifierStateTransition<E> stateTransition = modifierTransitions.get(modifier);
		return stateTransition != null? stateTransition : createDefaultModifierStateTransition(modifier);
	}

	private IModifierStateTransition<E> createDefaultModifierStateTransition(String modifier) {
		throw new UnsupportedOperationException(modifier);
	}

	private static <K, V> void insertOnce(Map<K, V> map, K key, V value) {
		final V prev = map.put(key, value);
		Preconditions.checkState(prev == null, "Duplicate value on key %s: %s -> %s", key, prev, value);
	}

	public MappedCompilerState<E> addStateTransition(String symbol, ISymbolCallStateTransition<E> transition) {
		insertOnce(symbolTransitions, symbol, transition);
		return this;
	}

	public MappedCompilerState<E> addStateTransition(String symbol, IModifierStateTransition<E> transition) {
		insertOnce(modifierTransitions, symbol, transition);
		return this;
	}
}
