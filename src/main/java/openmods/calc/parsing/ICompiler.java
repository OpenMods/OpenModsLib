package openmods.calc.parsing;

import openmods.calc.IExecutable;

public interface ICompiler<E> {
	public IExecutable<E> compile(Iterable<Token> input);
}
