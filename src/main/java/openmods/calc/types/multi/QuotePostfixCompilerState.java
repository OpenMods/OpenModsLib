package openmods.calc.types.multi;

import openmods.calc.IExecutable;
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
	protected IExecutable<TypedValue> parseToken(Token token) {
		if (token.type.isValue()) return Value.create(valueParser.parseToken(token));
		if (canBeRaw(token.type)) return Value.create(Symbol.get(domain, token.value));

		return rejectToken();
	}

}