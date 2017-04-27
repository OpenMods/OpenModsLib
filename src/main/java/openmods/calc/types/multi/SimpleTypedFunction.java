package openmods.calc.types.multi;

import openmods.calc.Frame;
import openmods.calc.symbol.ICallable;
import openmods.utils.OptionalInt;

public abstract class SimpleTypedFunction implements ICallable<TypedValue> {

	private final ICallable<TypedValue> handler;

	public SimpleTypedFunction(TypeDomain domain) {
		this.handler = TypedFunction.builder().addVariants(getClass()).build(domain, this);
	}

	@Override
	public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
		handler.call(frame, argumentsCount, returnsCount);
	}

}
