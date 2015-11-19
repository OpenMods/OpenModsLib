package openmods.calc;

public interface IOperator<E> extends IExecutable<E> {

	public enum Associativity {
		LEFT {
			@Override
			public <E> boolean compare(IOperator<E> left, IOperator<E> right) {
				return left.precedence() <= right.precedence();
			}
		},
		RIGHT {
			@Override
			public <E> boolean compare(IOperator<E> left, IOperator<E> right) {
				return left.precedence() < right.precedence();
			}
		};

		public abstract <E> boolean compare(IOperator<E> left, IOperator<E> right);
	}

	public int precedence();

	public Associativity getAssociativity();
}
