package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.ISymbol;

public class CallableWithValue implements ISymbol<TypedValue> {

	private final ICallable<TypedValue> callable;
	private final TypedValue value;

	public CallableWithValue(TypeDomain domain, ICallable<TypedValue> callable) {
		this.callable = callable;
		this.value = domain.create(ICallable.class, callable);
	}

	@Override
	public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		callable.call(frame, argumentsCount, returnsCount);
	}

	@Override
	public void get(Frame<TypedValue> frame) {
		frame.stack().push(value);
	}

}
