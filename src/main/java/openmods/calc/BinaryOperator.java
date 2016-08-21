package openmods.calc;

import openmods.utils.Stack;

public abstract class BinaryOperator<E> extends Operator<E> {

	public static final Associativity DEFAULT_ASSOCIATIVITY = Associativity.LEFT;

	public enum Associativity {
		LEFT {
			@Override
			protected boolean isLessThan(int left, int right) {
				return left <= right;
			}
		},
		RIGHT {
			@Override
			protected boolean isLessThan(int left, int right) {
				return left < right;
			}
		};

		protected abstract boolean isLessThan(int left, int right);
	}

	public final int precedence;

	public final Associativity associativity;

	public BinaryOperator(String id, int precedence, Associativity associativity) {
		super(id);
		this.precedence = precedence;
		this.associativity = associativity;
	}

	public BinaryOperator(String id, int precendence) {
		this(id, precendence, DEFAULT_ASSOCIATIVITY);
	}

	public abstract E execute(E left, E right);

	@Override
	public final void execute(Frame<E> frame) {
		final Stack<E> stack = frame.stack();

		final E right = stack.pop();
		final E left = stack.pop();
		final E result = execute(left, right);
		stack.push(result);
	}

	@Override
	public boolean isLessThan(Operator<E> other) {
		if (other instanceof UnaryOperator) return true; // unary operators always have higher precedence than binary

		final BinaryOperator<E> o = (BinaryOperator<E>)other;
		return associativity.isLessThan(precedence, o.precedence);
	}

	@Override
	public String toString() {
		return "BinaryOperator [" + id + "]";
	}

}
