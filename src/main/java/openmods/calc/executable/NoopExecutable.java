package openmods.calc.executable;

import openmods.calc.Frame;

public class NoopExecutable<E> implements IExecutable<E> {

	@Override
	public void execute(Frame<E> frame) {}

}