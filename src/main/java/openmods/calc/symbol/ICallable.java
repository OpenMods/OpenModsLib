package openmods.calc.symbol;

import openmods.calc.Frame;
import openmods.utils.OptionalInt;

public interface ICallable<E> {
	public void call(Frame<E> frame, OptionalInt argumentsCount, OptionalInt returnsCount);
}