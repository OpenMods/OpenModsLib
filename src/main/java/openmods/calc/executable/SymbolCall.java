package openmods.calc.executable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import openmods.calc.ExecutionErrorException;
import openmods.calc.Frame;
import openmods.calc.symbol.ISymbol;
import openmods.utils.OptionalInt;

public class SymbolCall<E> implements IExecutable<E> {
	public static final OptionalInt DEFAULT_ARG_COUNT = OptionalInt.absent();

	public static final OptionalInt DEFAULT_RET_COUNT = OptionalInt.absent();

	private final String id;

	private final OptionalInt argCount;

	private final OptionalInt returnCount;

	public SymbolCall(String id) {
		this(id, DEFAULT_ARG_COUNT, DEFAULT_RET_COUNT);
	}

	public SymbolCall(String id, int argumentCount, int returnCount) {
		this.id = id;
		this.argCount = OptionalInt.of(argumentCount);
		this.returnCount = OptionalInt.of(returnCount);
	}

	public SymbolCall(String id, OptionalInt argumentCount, OptionalInt returnCount) {
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
		} catch (ExecutionErrorException e) {
			throw e;
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

	private static String printOptional(OptionalInt value) {
		return value.isPresent()? String.valueOf(value.get()) : "?";
	}

	@Override
	public String toString() {
		return id + "[-" + printOptional(argCount) + "+" + printOptional(returnCount) + "]";
	}
}
