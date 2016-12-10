package openmods.calc.types.multi;

import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.ISymbol;
import openmods.utils.OptionalInt;

public class CallableWithValue implements ISymbol<TypedValue> {

	private final ICallable<TypedValue> callable;
	private final TypedValue value;

	public CallableWithValue(TypedValue value, ICallable<TypedValue> callable) {
		this.callable = callable;
		this.value = value;
	}

	@Override
	public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
		callable.call(frame, argumentsCount, returnsCount);
	}

	@Override
	public TypedValue get() {
		return value;
	}

}
