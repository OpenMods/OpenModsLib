package openmods.calc.executable;

import openmods.calc.Frame;

public interface IExecutable<E> {
	public void execute(Frame<E> frame);
}
