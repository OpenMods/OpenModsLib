package openmods.calc.parsing;

public interface ICompilerState<E> {

	public IAstParser<E> getParser();

	public ISymbolCallStateTransition<E> getStateForSymbolCall(String symbol);

	public IModifierStateTransition<E> getStateForModifier(String modifier);
}
