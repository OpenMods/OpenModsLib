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

	private BinaryOperator(String id, int precedence, Associativity associativity) {
		super(id);
		this.precedence = precedence;
		this.associativity = associativity;
	}

	private BinaryOperator(String id, int precendence) {
		this(id, precendence, DEFAULT_ASSOCIATIVITY);
	}

	public abstract static class Direct<E> extends BinaryOperator<E> {
		public Direct(String id, int precedence, Associativity associativity) {
			super(id, precedence, associativity);
		}

		public Direct(String id, int precendence) {
			super(id, precendence);
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
	}

	public abstract static class Scoped<E> extends BinaryOperator<E> {
		public Scoped(String id, int precedence, Associativity associativity) {
			super(id, precedence, associativity);
		}

		public Scoped(String id, int precendence) {
			super(id, precendence);
		}

		public abstract E execute(SymbolMap<E> symbols, E left, E right);

		@Override
		public final void execute(Frame<E> frame) {
			final Stack<E> stack = frame.stack();

			final E right = stack.pop();
			final E left = stack.pop();
			final E result = execute(frame.symbols(), left, right);
			stack.push(result);
		}
	}

	public abstract static class StackBased<E> extends BinaryOperator<E> {
		public StackBased(String id, int precedence, Associativity associativity) {
			super(id, precedence, associativity);
		}

		public StackBased(String id, int precendence) {
			super(id, precendence);
		}

		public abstract void executeOnStack(Frame<E> frame);

		@Override
		public final void execute(Frame<E> frame) {
			final Frame<E> executionFrame = FrameFactory.newLocalFrameWithSubstack(frame, 2);
			executeOnStack(executionFrame);
			executionFrame.stack().checkSizeIsExactly(1);
		}
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
