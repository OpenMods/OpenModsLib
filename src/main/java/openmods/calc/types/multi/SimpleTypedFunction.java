package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.Frame;
import openmods.calc.ICallable;

public abstract class SimpleTypedFunction implements ICallable<TypedValue> {

	private final TypedFunction handler;

	public SimpleTypedFunction(TypeDomain domain) {
		this.handler = TypedFunction.builder(domain).addVariants(this, getClass()).build();
	}

	@Override
	public void call(Frame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		handler.call(frame, argumentsCount, returnsCount);
	}

}
