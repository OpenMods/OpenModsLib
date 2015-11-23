package openmods.calc;

import com.google.common.base.Preconditions;

public class Variable<E> implements IStackSymbol<E> {

	private E value;

	public Variable(E value) {
		this.value = value;
	}

	@Override
	public void execute(CalculatorContext<E> context) {
		context.stack.push(value);
	}

	public E getValue() {
		return value;
	}

	public void setValue(E value) {
		this.value = value;
	}

	public static <E> Variable<E> create(E value) {
		return new Variable<E>(value);
	}

	@Override
	public void checkArgumentCount(int argCount) {
		Preconditions.checkArgument(argCount == 0, "Trying to call variable");
	}
}
