package openmods.calc;

import com.google.common.base.Preconditions;

public class SymbolGet<E> implements IExecutable<E> {

	private final String id;

	public SymbolGet(String id) {
		this.id = id;
	}

	@Override
	public void execute(Frame<E> frame) {
		final ISymbol<E> symbol = frame.symbols().get(id);
		Preconditions.checkNotNull(symbol, "Unknown symbol: %s", id);

		try {
			frame.stack().push(symbol.get());
		} catch (ExecutionErrorException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to get symbol '" + id + "'", e);
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SymbolGet) {
			final SymbolGet<?> other = (SymbolGet<?>)obj;
			return other.id.equals(this.id);
		}
		return false;
	}

	@Override
	public String toString() {
		return "@" + id;
	}
}
