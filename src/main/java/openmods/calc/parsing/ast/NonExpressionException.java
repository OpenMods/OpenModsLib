package openmods.calc.parsing.ast;

public class NonExpressionException extends IllegalStateException {
	private static final long serialVersionUID = -4520847078089446243L;

	public NonExpressionException() {}

	public NonExpressionException(String s) {
		super(s);
	}

}
