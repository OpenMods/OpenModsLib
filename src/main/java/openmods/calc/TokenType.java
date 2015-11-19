package openmods.calc;

public enum TokenType {
	FLOAT_NUMBER(true, false, false),
	DEC_NUMBER(true, false, false),
	HEX_NUMBER(true, false, false),
	OCT_NUMBER(true, false, false),
	BIN_NUMBER(true, false, false),
	QUOTED_NUMBER(true, false, false),

	SYMBOL(false, true, false),
	IMMEDIATE_SYMBOL(false, true, false),
	CONSTANT(false, true, false),

	OPERATOR(false, false, true),
	LEFT_BRACKET(false, false, true),
	RIGHT_BRACKET(false, false, false),
	SEPARATOR(false, false, true);

	public final boolean isValue;

	public final boolean isSymbol;

	public final boolean nextOpInfix;

	private TokenType(boolean isValue, boolean isSymbol, boolean nextOpInfix) {
		this.isValue = isValue;
		this.isSymbol = isSymbol;
		this.nextOpInfix = nextOpInfix;
	}

}