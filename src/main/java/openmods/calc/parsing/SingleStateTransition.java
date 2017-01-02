package openmods.calc.parsing;

import com.google.common.collect.PeekingIterator;

public abstract class SingleStateTransition<E> {

	public abstract IExprNode<E> parseSymbol(ICompilerState<E> state, PeekingIterator<Token> input);

	private final IAstParser<E> parser = new IAstParser<E>() {
		@Override
		public IExprNode<E> parse(ICompilerState<E> state, PeekingIterator<Token> input) {
			return SingleStateTransition.this.parseSymbol(state, input);
		}
	};

	private final ICompilerState<E> compilerState = new ICompilerState<E>() {
		@Override
		public IAstParser<E> getParser() {
			return parser;
		}

		@Override
		public ISymbolCallStateTransition<E> getStateForSymbolCall(String symbol) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IModifierStateTransition<E> getStateForModifier(String modifier) {
			throw new UnsupportedOperationException();
		}
	};

	public ICompilerState<E> getState() {
		return compilerState;
	}

	public static abstract class ForModifier<E> extends SingleStateTransition<E> implements IModifierStateTransition<E> {
		@Override
		public ICompilerState<E> getState() {
			return super.getState();
		}
	}

	public static abstract class ForSymbol<E> extends SingleStateTransition<E> implements ISymbolCallStateTransition<E> {
		@Override
		public ICompilerState<E> getState() {
			return super.getState();
		}
	}

}
