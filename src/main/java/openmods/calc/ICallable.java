package openmods.calc;

import openmods.utils.OptionalInt;

public interface ICallable<E> {
	public void call(Frame<E> frame, OptionalInt argumentsCount, OptionalInt returnsCount);
}