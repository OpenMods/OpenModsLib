package openmods.calc;

import com.google.common.base.Optional;

public abstract class FixedSymbol<E> implements ISymbol<E> {

	protected final int argCount;

	protected final int resultCount;

	public FixedSymbol(int argCount, int resultCount) {
		this.argCount = argCount;
		this.resultCount = resultCount;
	}

	@Override
	public final void execute(ICalculatorFrame<E> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
		if (argumentsCount.isPresent()) {
			final int args = argumentsCount.get();
			if (args != argCount) throw new StackValidationException("Expected %s argument(s) but got %s", this.argCount, args);
		}

		if (returnsCount.isPresent()) {
			final int returns = returnsCount.get();
			if (returns != resultCount) throw new StackValidationException("Expected %s result(s) but got %s", this.resultCount, returns);
		}

		execute(frame);
	}

	public abstract void execute(ICalculatorFrame<E> frame);
}
