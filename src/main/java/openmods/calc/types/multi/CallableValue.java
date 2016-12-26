package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import openmods.calc.Frame;
import openmods.calc.ICallable;
import openmods.calc.ISymbol;
import openmods.utils.OptionalInt;

public abstract class CallableValue {

	public static CallableValue from(final ICallable<TypedValue> callable) {
		return new CallableValue() {
			@Override
			public void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame) {
				callable.call(frame, argumentsCount, returnsCount);
			}

			@Override
			public ISymbol<TypedValue> toSymbol(TypeDomain domain) {
				final TypedValue self = selfValue(domain);
				return createSymbol(self, callable);
			}

			private ISymbol<TypedValue> createSymbol(final TypedValue self, final ICallable<TypedValue> callable) {
				return new ISymbol<TypedValue>() {
					@Override
					public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
						callable.call(frame, argumentsCount, returnsCount);
					}

					@Override
					public TypedValue get() {
						return self;
					}
				};
			}
		};
	}

	public abstract void call(TypedValue self, OptionalInt argumentsCount, OptionalInt returnsCount, Frame<TypedValue> frame);

	public ISymbol<TypedValue> toSymbol(TypeDomain domain) {
		final TypedValue self = selfValue(domain);
		return createSymbol(self);
	}

	public ISymbol<TypedValue> toSymbol(TypedValue value) {
		Preconditions.checkState(value.value == this);
		return createSymbol(value);
	}

	private ISymbol<TypedValue> createSymbol(final TypedValue self) {
		return new ISymbol<TypedValue>() {
			@Override
			public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
				CallableValue.this.call(self, argumentsCount, returnsCount, frame);
			}

			@Override
			public TypedValue get() {
				return self;
			}
		};
	}

	public TypedValue selfValue(TypeDomain domain) {
		return domain.create(CallableValue.class, this);
	}

	public static TypedValue wrap(TypeDomain domain, ICallable<TypedValue> callable) {
		return domain.create(CallableValue.class, from(callable));
	}
}