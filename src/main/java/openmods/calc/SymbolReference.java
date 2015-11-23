package openmods.calc;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class SymbolReference<E> implements ISymbol<E> {

	private final String id;

	private Optional<Integer> argumentCount;

	public SymbolReference(String id) {
		this.id = id;
		this.argumentCount = Optional.absent();
	}

	public SymbolReference(String id, int argumentCount) {
		this.id = id;
		this.argumentCount = Optional.of(argumentCount);
	}

	public void setArgumentCount(int argumentCount) {
		this.argumentCount = Optional.of(argumentCount);
	}

	@Override
	public void execute(CalculatorContext<E> context) {
		IExecutable<E> value = context.symbols.get(id);
		Preconditions.checkNotNull(value, "Unknown symbol: %s", id);
		if (argumentCount.isPresent()) {
			final int needs = argumentCount.get();
			if (value instanceof IStackSymbol) ((IStackSymbol<E>)value).checkArgumentCount(needs);

			final int has = context.stack.size();
			Preconditions.checkState(has >= needs, "Buffer underflow on symbol %s: needs %s but has %s", id, needs, has);
		}

		value.execute(context);
	}

	public static <E> SymbolReference<E> create(String id) {
		return new SymbolReference<E>(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode() * 31 + argumentCount.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SymbolReference) {
			SymbolReference<?> other = (SymbolReference<?>)obj;
			return other.id.equals(this.id) &&
					other.argumentCount.equals(this.argumentCount);
		}
		return false;
	}

	@Override
	public String toString() {
		return "DelayedSymbol [id=" + id + ", args=" + argumentCount + "]";
	}

}
