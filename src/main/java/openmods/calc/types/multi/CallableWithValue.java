package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.ISymbol;

public class CallableWithValue implements ISymbol<TypedValue> {

	private final ICallable<TypedValue> callable;
	private final TypedValue value;

	public CallableWithValue(TypedValue value, ICallable<TypedValue> callable) {
		this.callable = callable;
		this.value = value;
	}

	@Override
	public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		callable.call(frame, argumentsCount, returnsCount);
	}

	@Override
	public TypedValue get() {
		return value;
	}

}
