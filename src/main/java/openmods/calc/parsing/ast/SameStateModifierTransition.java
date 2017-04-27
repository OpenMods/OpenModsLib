package openmods.calc.parsing.ast;

public abstract class SameStateModifierTransition<N> implements IModifierStateTransition<N> {

	private final IParserState<N> parentState;

	public SameStateModifierTransition(IParserState<N> parentState) {
		this.parentState = parentState;
	}

	@Override
	public IParserState<N> getState() {
		return this.parentState;
	}

}
