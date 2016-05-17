package openmods.calc;

public interface IExecutable<E> {
	public void execute(ICalculatorFrame<E> frame);

	public String serialize();
}
