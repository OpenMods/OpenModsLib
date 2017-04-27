package openmods.calc.parsing.postfix;

import openmods.calc.parsing.token.Token;

public interface IPostfixParserState<E> {

	public enum Result {
		ACCEPTED,
		ACCEPTED_AND_FINISHED,
		REJECTED;
	}

	public Result acceptToken(Token token);

	public Result acceptChildResult(E result);

	public E getResult();

}
