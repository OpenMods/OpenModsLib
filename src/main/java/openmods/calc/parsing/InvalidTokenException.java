package openmods.calc.parsing;

public class InvalidTokenException extends IllegalArgumentException {
	private static final long serialVersionUID = -3774868349671963161L;

	public InvalidTokenException(Token token, Throwable cause) {
		super(token.toString(), cause);
	}

	public InvalidTokenException(Token token) {
		super(token.toString());
	}
}