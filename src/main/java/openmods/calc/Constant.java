package openmods.calc;

import com.google.common.base.Objects;

public class Constant<E> extends ValueSymbol<E> {

	private final E value;

	public Constant(E value) {
		this.value = value;
	}

	@Override
	public void get(ICalculatorFrame<E> frame) {
		frame.stack().push(value);
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
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return "Constant [value=" + value + "]";
	}
}
