package openmods.calc.types.multi;

import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.utils.OptionalInt;

public abstract class SimpleTypedFunction implements ICallable<TypedValue> {

	private final TypedFunction handler;

	public SimpleTypedFunction(TypeDomain domain) {
		this.handler = TypedFunction.builder(domain).addVariants(this, getClass()).build();
	}

	@Override
	public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
		handler.call(frame, argumentsCount, returnsCount);
	}

}
