package openmods.calc;

public class TokenUtils {

	public static Token t(TokenType type, String value) {
		return new Token(type, value);
	}

	public static Token dec(String value) {
		return t(TokenType.DEC_NUMBER, value);
	}

	public static Token f(String value) {
		return t(TokenType.FLOAT_NUMBER, value);
	}

	public static Token oct(String value) {
		return t(TokenType.OCT_NUMBER, value);
	}

	public static Token hex(String value) {
		return t(TokenType.HEX_NUMBER, value);
	}

	public static Token bin(String value) {
		return t(TokenType.BIN_NUMBER, value);
	}

	public static Token quoted(String value) {
		return t(TokenType.QUOTED_NUMBER, value);
	}

	public static Token symbol(String value) {
		return t(TokenType.SYMBOL, value);
	}

	public static Token constant(String value) {
		return t(TokenType.CONSTANT, value);
	}

	public static Token op(String value) {
		return t(TokenType.OPERATOR, value);
	}

	public static final Token COMMA = t(TokenType.SEPARATOR, ",");
	public static final Token RIGHT_BRACKET = t(TokenType.RIGHT_BRACKET, ")");
	public static final Token LEFT_BRACKET = t(TokenType.LEFT_BRACKET, "(");
}
