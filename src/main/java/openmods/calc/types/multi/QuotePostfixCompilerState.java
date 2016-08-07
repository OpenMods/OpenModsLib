package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import openmods.calc.IExecutable;
import openmods.calc.Value;
import openmods.calc.parsing.IPostfixCompilerState;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;

public class QuotePostfixCompilerState implements IPostfixCompilerState<TypedValue> {
	private IExecutable<TypedValue> result;

	private final IValueParser<TypedValue> valueParser;
	private final TypeDomain domain;

	public QuotePostfixCompilerState(IValueParser<TypedValue> valueParser, TypeDomain domain) {
		this.valueParser = valueParser;
		this.domain = domain;
	}

	private static boolean canBeRaw(TokenType type) {
		return type == TokenType.MODIFIER || type == TokenType.OPERATOR || type == TokenType.SYMBOL;
	}

	@Override
	public Result acceptToken(Token token) {
		Preconditions.checkState(result == null);

		final TypedValue resultValue;
		if (token.type.isValue()) resultValue = valueParser.parseToken(token);
		else if (canBeRaw(token.type)) resultValue = domain.create(Symbol.class, Symbol.get(token.value));
		else return Result.REJECTED;
		result = Value.create(resultValue);

		return Result.ACCEPTED_AND_FINISHED;
	}

	@Override
	public Result acceptExecutable(IExecutable<TypedValue> executable) {
		return Result.REJECTED;
	}

	@Override
	public IExecutable<TypedValue> exit() {
		Preconditions.checkState(result != null);
		return result;
	}
}