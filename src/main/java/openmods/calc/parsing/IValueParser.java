package openmods.calc.parsing;

public interface IValueParser<E> {
	public E parseToken(Token token);
}
