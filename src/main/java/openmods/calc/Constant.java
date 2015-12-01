package openmods.calc;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class Constant<E> implements ISymbol<E> {

	private final E value;

	public Constant(E value) {
		this.value = value;
	}

	@Override
	public void execute(ICalculatorFrame<E> frame) {
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

	@Override
	public void checkArgumentCount(int argCount) {
		Preconditions.checkArgument(argCount == 0, "Trying to call constant with %s args", argCount);
	}

	@Override
	public void checkResultCount(int resultCount) {
		Preconditions.checkArgument(resultCount == 1, "Invalid result count: %s", resultCount);
	}
}
