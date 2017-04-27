package openmods.calc.parsing;

import openmods.calc.parsing.token.Token;

public interface IValueParser<E> {
	public E parseToken(Token token);
}
