package openmods.calc;

import openmods.calc.parsing.Token;

public interface IValueParser<E> {
	public E parseToken(Token token);
}
