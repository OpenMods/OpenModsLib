package openmods.calc;

public class NoopExecutable<E> implements IExecutable<E> {

	@Override
	public void execute(Frame<E> frame) {}

	@Override
	public String serialize() {
		return "<nop>";
	}

}