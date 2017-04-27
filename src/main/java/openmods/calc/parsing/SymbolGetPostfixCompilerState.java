package openmods.calc.parsing;

import openmods.calc.executable.IExecutable;
import openmods.calc.executable.SymbolGet;
import openmods.calc.parsing.postfix.SingleTokenPostfixParserState;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;

public class SymbolGetPostfixCompilerState<E> extends SingleTokenPostfixParserState<IExecutable<E>> {
	@Override
	protected IExecutable<E> parseToken(Token token) {
		if (token.type == TokenType.SYMBOL)
			return new SymbolGet<E>(token.value);

		return rejectToken();
	}
}