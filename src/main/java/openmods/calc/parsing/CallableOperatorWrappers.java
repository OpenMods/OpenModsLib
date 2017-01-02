package openmods.calc.parsing;

import openmods.calc.BinaryOperator;
import openmods.calc.FixedCallable;
import openmods.calc.Frame;
import openmods.calc.UnaryOperator;
import openmods.calc.types.multi.TypedValue;

public class CallableOperatorWrappers {

	public static class Binary extends FixedCallable<TypedValue> {
		private final BinaryOperator<TypedValue> op;

		public Binary(BinaryOperator<TypedValue> op) {
			super(2, 1);
			this.op = op;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			op.execute(frame);
		}
	}

	public static class Unary extends FixedCallable<TypedValue> {
		private final UnaryOperator<TypedValue> op;

		public Unary(UnaryOperator<TypedValue> op) {
			super(1, 1);
			this.op = op;
		}

		@Override
		public void call(Frame<TypedValue> frame) {
			op.execute(frame);
		}
	}

}
