package openmods.calc.parsing.ast;

public abstract class SameStateSymbolTransition<N> implements ISymbolCallStateTransition<N> {

	private final IParserState<N> parentState;

	public SameStateSymbolTransition(IParserState<N> parentState) {
		this.parentState = parentState;
	}

	@Override
	public IParserState<N> getState() {
		return this.parentState;
	}

}
