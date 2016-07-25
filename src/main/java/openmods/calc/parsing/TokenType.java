package openmods.calc.parsing;

import java.util.EnumSet;
import java.util.Set;

enum TokenProperties {
	VALUE,
	SYMBOL,
	EXPRESSION_TERMINATOR
}

public enum TokenType {
	DEC_NUMBER(TokenProperties.VALUE),
	HEX_NUMBER(TokenProperties.VALUE),
	OCT_NUMBER(TokenProperties.VALUE),
	BIN_NUMBER(TokenProperties.VALUE),
	QUOTED_NUMBER(TokenProperties.VALUE),

	STRING(TokenProperties.VALUE),

	SYMBOL(TokenProperties.SYMBOL),
	SYMBOL_WITH_ARGS(TokenProperties.SYMBOL),

	OPERATOR(),

	LEFT_BRACKET(),
	SEPARATOR(TokenProperties.EXPRESSION_TERMINATOR),
	RIGHT_BRACKET(TokenProperties.EXPRESSION_TERMINATOR),

	MODIFIER();

	public boolean isValue() {
		return properties.contains(TokenProperties.VALUE);
	}

	public final boolean isSymbol() {
		return properties.contains(TokenProperties.SYMBOL);
	}

	public boolean isExpressionTerminator() {
		return properties.contains(TokenProperties.EXPRESSION_TERMINATOR);
	}

	private final Set<TokenProperties> properties;

	private TokenType(TokenProperties property, TokenProperties... properties) {
		this.properties = EnumSet.of(property, properties);
	}

	private TokenType() {
		this.properties = EnumSet.noneOf(TokenProperties.class);
	}
}