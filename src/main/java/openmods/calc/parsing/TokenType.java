package openmods.calc.parsing;

import java.util.EnumSet;
import java.util.Set;

enum TokenProperties {
	VALUE,
	SYMBOL,
	NEXT_OP_UNARY,
	INSERT_DEFAULT_OP_LEFT,
	INSERT_DEFAULT_OP_RIGHT,
	CALL_START
}

public enum TokenType {
	DEC_NUMBER(TokenProperties.VALUE, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.INSERT_DEFAULT_OP_RIGHT),
	HEX_NUMBER(TokenProperties.VALUE, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.INSERT_DEFAULT_OP_RIGHT),
	OCT_NUMBER(TokenProperties.VALUE, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.INSERT_DEFAULT_OP_RIGHT),
	BIN_NUMBER(TokenProperties.VALUE, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.INSERT_DEFAULT_OP_RIGHT),
	QUOTED_NUMBER(TokenProperties.VALUE, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.INSERT_DEFAULT_OP_RIGHT),

	STRING(TokenProperties.VALUE, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.INSERT_DEFAULT_OP_RIGHT),

	SYMBOL(TokenProperties.SYMBOL, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.INSERT_DEFAULT_OP_RIGHT),
	SYMBOL_WITH_ARGS(TokenProperties.SYMBOL),

	OPERATOR(TokenProperties.NEXT_OP_UNARY),
	LEFT_BRACKET(TokenProperties.NEXT_OP_UNARY, TokenProperties.INSERT_DEFAULT_OP_LEFT, TokenProperties.CALL_START),
	RIGHT_BRACKET(TokenProperties.INSERT_DEFAULT_OP_RIGHT),
	SEPARATOR(TokenProperties.NEXT_OP_UNARY),
	MODIFIER();

	public boolean isValue() {
		return properties.contains(TokenProperties.VALUE);
	}

	public final boolean isSymbol() {
		return properties.contains(TokenProperties.SYMBOL);
	}

	public final boolean isNextOpUnary() {
		return properties.contains(TokenProperties.NEXT_OP_UNARY);
	}

	public final boolean canInsertDefaultOpOnLeft() {
		return properties.contains(TokenProperties.INSERT_DEFAULT_OP_LEFT);
	}

	public final boolean canInsertDefaultOpOnRight() {
		return properties.contains(TokenProperties.INSERT_DEFAULT_OP_RIGHT);
	}

	public final boolean isCallStart() {
		return properties.contains(TokenProperties.CALL_START);
	}

	private final Set<TokenProperties> properties;

	private TokenType(TokenProperties property, TokenProperties... properties) {
		this.properties = EnumSet.of(property, properties);
	}

	private TokenType() {
		this.properties = EnumSet.noneOf(TokenProperties.class);
	}
}