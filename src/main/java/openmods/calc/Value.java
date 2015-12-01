package openmods.calc;

import com.google.common.base.Objects;

public class Value<E> implements IExecutable<E> {

	private final E value;

	public Value(E value) {
		this.value = value;
	}

	@Override
	public void execute(ICalculatorFrame<E> frame) {
		frame.stack().push(value);
	}

	public static <E> Value<E> create(E value) {
		return new Value<E>(value);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Value) && Objects.equal(((Value<?>)other).value, this.value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return "Value [value=" + value + "]";
	}
}
