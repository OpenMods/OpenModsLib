package openmods.calc.types.multi;

import com.google.common.base.Optional;
import openmods.calc.ICalculatorFrame;
import openmods.calc.ISymbol;

public abstract class SimpleTypedFunction implements ISymbol<TypedValue> {

	private final TypedFunction handler;

	public SimpleTypedFunction(TypeDomain domain) {
		this.handler = TypedFunction.builder(domain).addVariants(this, getClass()).build();
	}

	@Override
	public void execute(ICalculatorFrame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		handler.execute(frame, argumentsCount, returnsCount);
	}

}
