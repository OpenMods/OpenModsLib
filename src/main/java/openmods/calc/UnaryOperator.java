package openmods.calc;

public abstract class UnaryOperator<E> extends Operator<E> {

	public UnaryOperator(int precendence, Associativity associativity) {
		super(precendence, associativity);
	}

	public UnaryOperator(int precendence) {
		super(precendence);
	}

	protected abstract E execute(E value);

	@Override
	public void execute(CalculatorContext<E> context) {
		final E value = context.stack.pop();
		final E result = execute(value);
		context.stack.push(result);
	}

}
