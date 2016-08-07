package openmods.calc;

public class NoopExecutable<E> implements IExecutable<E> {

	@Override
	public void execute(ICalculatorFrame<E> frame) {}

	@Override
	public String serialize() {
		return "<nop>";
	}

}