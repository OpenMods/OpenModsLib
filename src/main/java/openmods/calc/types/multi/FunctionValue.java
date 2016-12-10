package openmods.calc.types.multi;

import openmods.calc.ICallable;

public class FunctionValue {
	public final ICallable<TypedValue> callable;

	public FunctionValue(ICallable<TypedValue> callable) {
		this.callable = callable;
	}

	public static TypedValue wrap(TypeDomain domain, ICallable<TypedValue> callable) {
		return domain.create(FunctionValue.class, new FunctionValue(callable));
	}
}