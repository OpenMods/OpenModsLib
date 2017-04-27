package openmods.calc.parsing.ast;

public interface IModifierStateTransition<N> {
	public IParserState<N> getState();

	public N createRootNode(N child);
}