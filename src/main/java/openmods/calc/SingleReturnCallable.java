package openmods.calc;

import com.google.common.base.Optional;
import openmods.calc.types.multi.TypedCalcUtils;

public abstract class SingleReturnCallable<E> implements ICallable<E> {

	@Override
	public final void call(Frame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		TypedCalcUtils.expectSingleReturn(returnsCount);

		final E result = call(frame, argumentsCount);
		frame.stack().push(result);
	}

	public abstract E call(Frame<E> frame, Optional<Integer> argumentsCount);
}
