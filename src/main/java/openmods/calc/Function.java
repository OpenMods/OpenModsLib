package openmods.calc;

public abstract class Function<E> implements ISymbol<E> {

	protected final int argCount;

	protected final int resultCount;

	public Function(int argCount, int resultCount) {
		this.argCount = argCount;
		this.resultCount = resultCount;
	}

	@Override
	public final void checkArgumentCount(int argCount) {
		if (this.argCount != argCount) throw new StackValidationException("Expected %s argument(s) but got %s", this.argCount, argCount);
	}

	@Override
	public final void checkResultCount(int resultCount) {
		if (this.resultCount != resultCount) throw new StackValidationException("Expected %s result(s) but got %s", this.resultCount, resultCount);
	}
}
