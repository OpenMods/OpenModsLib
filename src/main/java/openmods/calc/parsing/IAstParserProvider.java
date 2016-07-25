package openmods.calc.parsing;

public interface IAstParserProvider<E> {
	public IAstParser<E> getParser();
}
