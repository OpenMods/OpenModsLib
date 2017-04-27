package openmods.calc.parsing.ast;

public class UnmatchedBracketsException extends IllegalStateException {
	private static final long serialVersionUID = -6778162650626172231L;

	public UnmatchedBracketsException() {
		super();
	}

	public UnmatchedBracketsException(String bracket) {
		super("bracket = " + bracket);
	}

	public UnmatchedBracketsException(String left, String right) {
		super("left = " + left + ", right = " + right);
	}
}
