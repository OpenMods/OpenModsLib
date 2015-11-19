package openmods.calc;

import com.google.common.base.Objects;

public class Constant<E> implements ISymbol<E> {

	private final E value;

	public Constant(E value) {
		this.value = value;
	}

	@Override
	public void execute(CalculatorContext<E> context) {
		context.stack.push(value);
	}

	public static <E> Constant<E> create(E value) {
		return new Constant<E>(value);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Constant) && Objects.equal(((Constant<?>)other).value, this.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return "Constant [value=" + value + "]";
	}

}
