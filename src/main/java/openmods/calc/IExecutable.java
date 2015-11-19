package openmods.calc;

public interface IExecutable<E> {
	public void execute(CalculatorContext<E> context);
}
