package openmods.calc.types.multi;

import openmods.calc.ICallable;

public class CallableValue {
	public final ICallable<TypedValue> callable;

	public CallableValue(ICallable<TypedValue> callable) {
		this.callable = callable;
	}

	public static TypedValue wrap(TypeDomain domain, ICallable<TypedValue> callable) {
		return domain.create(CallableValue.class, new CallableValue(callable));
	}
}