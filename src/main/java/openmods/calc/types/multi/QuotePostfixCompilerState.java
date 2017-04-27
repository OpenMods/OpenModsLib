package openmods.calc.types.multi;

import openmods.calc.executable.IExecutable;
import openmods.calc.executable.Value;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.postfix.SingleTokenPostfixParserState;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenType;

public class QuotePostfixCompilerState extends SingleTokenPostfixParserState<IExecutable<TypedValue>> {

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