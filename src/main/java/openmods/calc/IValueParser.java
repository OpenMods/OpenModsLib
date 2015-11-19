package openmods.calc;

public interface IValueParser<E> {
	public E parseToken(Token token);
}
