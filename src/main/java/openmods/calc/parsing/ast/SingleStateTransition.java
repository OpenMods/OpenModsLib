package openmods.calc.parsing.ast;

import com.google.common.collect.PeekingIterator;
import openmods.calc.parsing.token.Token;

public abstract class SingleStateTransition<N> {

	public abstract N parseSymbol(IParserState<N> state, PeekingIterator<Token> input);

	private final IAstParser<N> parser = new IAstParser<N>() {
		@Override
		public N parse(IParserState<N> state, PeekingIterator<Token> input) {
			return SingleStateTransition.this.parseSymbol(state, input);
		}
	};

	private final IParserState<N> compilerState = new IParserState<N>() {
		@Override
		public IAstParser<N> getParser() {
			return parser;
		}

		@Override
		public ISymbolCallStateTransition<N> getStateForSymbolCall(String symbol) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IModifierStateTransition<N> getStateForModifier(String modifier) {
			throw new UnsupportedOperationException();
		}
	};

	public IParserState<N> getState() {
		return compilerState;
	}

	public static abstract class ForModifier<N> extends SingleStateTransition<N> implements IModifierStateTransition<N> {
		@Override
		public IParserState<N> getState() {
			return super.getState();
		}
	}

	public static abstract class ForSymbol<N> extends SingleStateTransition<N> implements ISymbolCallStateTransition<N> {
		@Override
		public IParserState<N> getState() {
			return super.getState();
		}
	}

}
