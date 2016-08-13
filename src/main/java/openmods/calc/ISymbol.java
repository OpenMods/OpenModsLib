package openmods.calc;

import com.google.common.base.Optional;

public interface ISymbol<E> {
	public void call(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount);

	public void get(ICalculatorFrame<E> frame);
}
