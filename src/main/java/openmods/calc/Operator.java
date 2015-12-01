package openmods.calc;

public abstract class Operator<E> implements IExecutable<E> {

	public enum Associativity {
		LEFT {
			@Override
			public <E> boolean compare(Operator<E> left, Operator<E> right) {
				return left.precedence <= right.precedence;
			}
		},
		RIGHT {
			@Override
			public <E> boolean compare(Operator<E> left, Operator<E> right) {
				return left.precedence < right.precedence;
			}
		};

		public abstract <E> boolean compare(Operator<E> left, Operator<E> right);
	}

	public final int precedence;

	public final Associativity associativity;

	public Operator(int precedence, Associativity associativity) {
		this.precedence = precedence;
		this.associativity = associativity;
	}

	public Operator(int precendence) {
		this(precendence, Associativity.LEFT);
	}
}
