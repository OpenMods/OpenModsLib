package openmods.calc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class SymbolReference<E> implements IExecutable<E> {

	private final String id;

	private static class StackParams {

		public final int argCount;
		public final int resultCount;

		public StackParams(int argCount, int resultCount) {
			this.argCount = argCount;
			this.resultCount = resultCount;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + argCount;
			result = prime * result + resultCount;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof StackParams) {
				final StackParams other = (StackParams)obj;
				return other.argCount == this.argCount &&
						other.resultCount == this.resultCount;
			}
			return false;
		}

		@Override
		public String toString() {
			return "[-" + argCount + "+" + resultCount + "]";
		}

	}

	private Optional<StackParams> stackParams;

	public SymbolReference(String id) {
		this.id = id;
		this.stackParams = Optional.absent();
	}

	public SymbolReference(String id, int argumentCount, int resultCount) {
		this.id = id;
		this.stackParams = Optional.of(new StackParams(argumentCount, resultCount));
	}

	public void setStackParams(int argumentCount, int resultCount) {
		this.stackParams = Optional.of(new StackParams(argumentCount, resultCount));
	}

	@Override
	public void execute(ICalculatorFrame<E> frame) {
		final ISymbol<E> symbol = frame.getSymbol(id);
		Preconditions.checkNotNull(symbol, "Unknown symbol: %s", id);

		if (stackParams.isPresent()) {
			final StackParams params = stackParams.get();
			symbol.checkArgumentCount(params.argCount);
			symbol.checkResultCount(params.resultCount);
		}

		symbol.execute(frame);
	}

	public static <E> SymbolReference<E> create(String id) {
		return new SymbolReference<E>(id);
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + id.hashCode();
		result = 31 * result + stackParams.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SymbolReference) {
			final SymbolReference<?> other = (SymbolReference<?>)obj;
			return other.id.equals(this.id) &&
					other.stackParams.equals(this.stackParams);
		}
		return false;
	}

	@Override
	public String toString() {
		return "SymbolReference [id=" + id + ", stackParams=" + stackParams + "]";
	}
}
