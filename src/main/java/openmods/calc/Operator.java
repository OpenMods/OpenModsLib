package openmods.calc;

public abstract class Operator<E> implements IOperator<E> {

	private final int precendence;

	private final Associativity associativity;

	public Operator(int precendence, Associativity associativity) {
		this.precendence = precendence;
		this.associativity = associativity;
	}

	public Operator(int precendence) {
		this(precendence, Associativity.LEFT);
	}

	@Override
	public int precedence() {
		return precendence;
	}

	@Override
	public Associativity getAssociativity() {
		return associativity;
	}
}
