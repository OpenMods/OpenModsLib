package openmods.calc;

import openmods.utils.OptionalInt;

public abstract class FixedCallable<E> implements ICallable<E> {

	protected final int argCount;

	protected final int resultCount;

	public FixedCallable(int argCount, int resultCount) {
		this.argCount = argCount;
		this.resultCount = resultCount;
	}

	@Override
	public final void call(Frame<E> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
		if (!argumentsCount.compareIfPresent(argCount)) throw new StackValidationException("Expected %s argument(s) but got %s", this.argCount, argumentsCount.get());
		if (!returnsCount.compareIfPresent(resultCount)) throw new StackValidationException("Has %s result(s) but expected %s", this.resultCount, returnsCount.get());

		call(frame);
	}

	public abstract void call(Frame<E> frame);

}
