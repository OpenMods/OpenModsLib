package openmods.calc;

public class Variable<E> implements ISymbol<E> {

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
}
