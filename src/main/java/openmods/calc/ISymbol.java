package openmods.calc;

import com.google.common.base.Optional;

public interface ISymbol<E> {
	public void execute(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount);
}
