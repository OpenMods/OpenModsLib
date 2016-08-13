package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.FunctionSymbol;
import openmods.calc.ICalculatorFrame;

public abstract class SimpleTypedFunction extends FunctionSymbol<TypedValue> {

	private final TypedFunction handler;

	public SimpleTypedFunction(TypeDomain domain) {
		this.handler = TypedFunction.builder(domain).addVariants(this, getClass()).build();
	}

	@Override
	public void call(ICalculatorFrame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		handler.call(frame, argumentsCount, returnsCount);
	}

}
