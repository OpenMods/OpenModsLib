package openmods.calc;

import com.google.common.base.Optional;

public abstract class FixedCallable<E> implements ICallable<E> {

	protected final int argCount;

	protected final int resultCount;

	public FixedCallable(int argCount, int resultCount) {
		this.argCount = argCount;
		this.resultCount = resultCount;
	}

	@Override
	public final void call(Frame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		if (argumentsCount.isPresent()) {
			final int args = argumentsCount.get();
			if (args != argCount) throw new StackValidationException("Expected %s argument(s) but got %s", this.argCount, args);
		}

		if (returnsCount.isPresent()) {
			final int returns = returnsCount.get();
			if (returns != resultCount) throw new StackValidationException("Has %s result(s) but expected %s", this.resultCount, returns);
		}

		call(frame);
	}

	public abstract void call(Frame<E> frame);

}
