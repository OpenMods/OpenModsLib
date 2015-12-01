package openmods.calc;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class SymbolReference<E> implements IExecutable<E> {

	private final String id;

	private Optional<Integer> argCount;

	private Optional<Integer> returnCount;

	public SymbolReference(String id) {
		this.id = id;
		this.argCount = Optional.absent();
		this.returnCount = Optional.absent();
	}

	public SymbolReference(String id, int argumentCount, int returnCount) {
		this.id = id;
		this.argCount = Optional.of(argumentCount);
		this.returnCount = Optional.of(returnCount);
	}

	public SymbolReference<?> setArgumentsCount(int count) {
		this.argCount = Optional.of(count);
		return this;
	}

	public SymbolReference<?> setReturnsCount(int count) {
		this.returnCount = Optional.of(count);
		return this;
	}

	@Override
	public void execute(ICalculatorFrame<E> frame) {
		final ISymbol<E> symbol = frame.getSymbol(id);
		Preconditions.checkNotNull(symbol, "Unknown symbol: %s", id);

		try {
			symbol.execute(frame, argCount, returnCount);
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute symbol '" + id + "'", e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, argCount, returnCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SymbolReference) {
			final SymbolReference<?> other = (SymbolReference<?>)obj;
			return other.id.equals(this.id) &&
					other.argCount.equals(this.argCount) &&
					other.returnCount.equals(this.returnCount);
		}
		return false;
	}

	private static <T> String printOptional(Optional<T> value) {
		return value.isPresent()? String.valueOf(value.get()) : "?";
	}

	@Override
	public String toString() {
		return id + "[-" + printOptional(argCount) + "+" + printOptional(returnCount) + "]";
	}
}
