package openmods.calc;

public abstract class BinaryOperator<E> extends Operator<E> {

	public BinaryOperator(int precendence, Associativity associativity) {
		super(precendence, associativity);
	}

	public BinaryOperator(int precendence) {
		super(precendence);
	}

	protected abstract E execute(E left, E right);

	@Override
	public void execute(CalculatorContext<E> context) {
		final E right = context.stack.pop();
		final E left = context.stack.pop();
		final E result = execute(left, right);
		context.stack.push(result);
	}

}
