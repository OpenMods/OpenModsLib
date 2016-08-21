package openmods.calc;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class SymbolCall<E> implements IExecutable<E> {
	public static final Optional<Integer> DEFAULT_ARG_COUNT = Optional.absent();

	public static final Optional<Integer> DEFAULT_RET_COUNT = Optional.absent();

	private final String id;

	private final Optional<Integer> argCount;

	private final Optional<Integer> returnCount;

	public SymbolCall(String id) {
		this(id, DEFAULT_ARG_COUNT, DEFAULT_RET_COUNT);
	}

	public SymbolCall(String id, int argumentCount, int returnCount) {
		this.id = id;
		this.argCount = Optional.of(argumentCount);
		this.returnCount = Optional.of(returnCount);
	}

	public SymbolCall(String id, Optional<Integer> argumentCount, Optional<Integer> returnCount) {
		this.id = id;
		this.argCount = argumentCount;
		this.returnCount = returnCount;
	}

	@Override
	public void execute(Frame<E> frame) {
		final ISymbol<E> symbol = frame.symbols().get(id);
		Preconditions.checkNotNull(symbol, "Unknown symbol: %s", id);

		try {
			symbol.call(frame, argCount, returnCount);
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
		if (obj instanceof SymbolCall) {
			final SymbolCall<?> other = (SymbolCall<?>)obj;
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

	@Override
	public String serialize() {
		if (argCount.isPresent()) {
			final int a = argCount.get();
			if (returnCount.isPresent()) {
				final int r = returnCount.get();
				return id + "@" + a + "," + r;
			} else {
				return id + "@" + a;
			}
		} else if (returnCount.isPresent()) {
			final int r = returnCount.get();
			return id + "@," + r;
		} else {
			return id;
		}
	}
}
