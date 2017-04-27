package openmods.calc.symbol;

import openmods.calc.Frame;
import openmods.calc.executable.Operator;
import openmods.calc.types.multi.TypedValue;

public class CallableOperatorWrapper extends FixedCallable<TypedValue> {

	private final Operator<TypedValue> op;

	public CallableOperatorWrapper(Operator<TypedValue> op) {
		super(op.arity().args, 1);
		this.op = op;
	}

	@Override
	public void call(Frame<TypedValue> frame) {
		op.execute(frame);
	}

}
