package openmods.calc.parsing;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import openmods.utils.CollectionUtils;

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
				return createDefaultSymbolNode(symbol, children);
			}
		};
	}

	protected IExprNode<E> createDefaultSymbolNode(String symbol, List<IExprNode<E>> children) {
		return new SymbolCallNode<E>(symbol, children);
	}

	@Override
	public IModifierStateTransition<E> getStateForModifier(String modifier) {
		final IModifierStateTransition<E> stateTransition = modifierTransitions.get(modifier);
		return stateTransition != null? stateTransition : createDefaultModifierStateTransition(modifier);
	}

	private IModifierStateTransition<E> createDefaultModifierStateTransition(String modifier) {
		throw new UnsupportedOperationException(modifier);
	}

	public MappedCompilerState<E> addStateTransition(String symbol, ISymbolCallStateTransition<E> transition) {
		CollectionUtils.putOnce(symbolTransitions, symbol, transition);
		return this;
	}

	public MappedCompilerState<E> addStateTransition(String symbol, IModifierStateTransition<E> transition) {
		CollectionUtils.putOnce(modifierTransitions, symbol, transition);
		return this;
	}
}
