package openmods.calc.parsing.ast;

public interface IParserState<N> {

	public IAstParser<N> getParser();

	public ISymbolCallStateTransition<N> getStateForSymbolCall(String symbol);

	public IModifierStateTransition<N> getStateForModifier(String modifier);
}
