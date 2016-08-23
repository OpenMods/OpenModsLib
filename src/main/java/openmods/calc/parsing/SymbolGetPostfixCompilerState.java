package openmods.calc.parsing;

import com.google.common.base.Optional;
import openmods.calc.IExecutable;
import openmods.calc.SymbolGet;

public class SymbolGetPostfixCompilerState<E> extends SingleTokenPostfixCompilerState<E> {
	@Override
	protected Optional<? extends IExecutable<E>> parseToken(Token token) {
		if (token.type == TokenType.SYMBOL)
			return Optional.of(new SymbolGet<E>(token.value));
		else
			return Optional.absent();
	}
}