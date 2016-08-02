package openmods.calc;

public enum ExprType {
	PREFIX(true),
	INFIX(true),
	POSTFIX(false);

	public final boolean hasSingleResult;

	private ExprType(boolean hasSingleResult) {
		this.hasSingleResult = hasSingleResult;
	}
}