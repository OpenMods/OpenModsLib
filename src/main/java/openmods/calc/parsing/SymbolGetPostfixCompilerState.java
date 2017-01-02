package openmods.calc.parsing;

import openmods.calc.IExecutable;
import openmods.calc.SymbolGet;

public class SymbolGetPostfixCompilerState<E> extends SingleTokenPostfixCompilerState<E> {
	@Override
	protected IExecutable<E> parseToken(Token token) {
		if (token.type == TokenType.SYMBOL)
			return new SymbolGet<E>(token.value);

		return rejectToken();
	}
}