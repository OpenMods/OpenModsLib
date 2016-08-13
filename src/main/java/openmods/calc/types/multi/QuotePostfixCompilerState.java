package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.Value;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.SingleTokenPostfixCompilerState;
import openmods.calc.parsing.Token;
import openmods.calc.parsing.TokenType;

public class QuotePostfixCompilerState extends SingleTokenPostfixCompilerState<TypedValue> {

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
	protected Optional<Value<TypedValue>> parseToken(Token token) {
		final TypedValue resultValue;
		if (token.type.isValue()) resultValue = valueParser.parseToken(token);
		else if (canBeRaw(token.type)) resultValue = domain.create(Symbol.class, Symbol.get(token.value));
		else return Optional.absent();
		return Optional.of(Value.create(resultValue));
	}

}