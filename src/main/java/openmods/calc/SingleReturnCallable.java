package openmods.calc;

import com.google.common.base.Optional;

public abstract class SingleReturnCallable<E> implements ICallable<E> {

	@Override
	public final void call(Frame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		if (returnsCount.isPresent()) {
			final int returns = returnsCount.get();
			if (returns != 1) throw new StackValidationException("Has single result but expected %s", returns);
		}

		final E result = call(frame, argumentsCount);
		frame.stack().push(result);
	}

	public abstract E call(Frame<E> frame, Optional<Integer> argumentsCount);
}
