package openmods.calc;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class DelayedSymbol<E> implements ISymbol<E> {

	private final String id;

	public DelayedSymbol(String id) {
		this.id = id;
	}

	@Override
	public void execute(CalculatorContext<E> context) {
		IExecutable<E> value = context.symbols.get(id);
		Preconditions.checkNotNull(value, "Unknown symbol: %s", id);
		value.execute(context);
	}

	public static <E> DelayedSymbol<E> create(String id) {
		return new DelayedSymbol<E>(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof DelayedSymbol) && Objects.equal(((DelayedSymbol<?>)other).id, this.id);
	}

	@Override
	public String toString() {
		return "Symbol [id=" + id + "]";
	}

}
