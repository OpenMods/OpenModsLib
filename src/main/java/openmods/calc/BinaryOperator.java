package openmods.calc;

import openmods.utils.Stack;

public abstract class BinaryOperator<E> extends Operator<E> {

	public enum Associativity {
		LEFT {
			@Override
			protected <E> boolean isLessThan(int left, int right) {
				return left <= right;
			}
		},
		RIGHT {
			@Override
			protected <E> boolean isLessThan(int left, int right) {
				return left < right;
			}
		};

		protected abstract <E> boolean isLessThan(int left, int right);
	}

	public final int precedence;

	public final Associativity associativity;

	public BinaryOperator(int precedence, Associativity associativity) {
		this.precedence = precedence;
		this.associativity = associativity;
	}

	public BinaryOperator(int precendence) {
		this(precendence, Associativity.LEFT);
	}

	protected abstract E execute(E left, E right);

	@Override
	public final void execute(ICalculatorFrame<E> frame) {
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

}
