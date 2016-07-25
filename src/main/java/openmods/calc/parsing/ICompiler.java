package openmods.calc.parsing;

import com.google.common.collect.PeekingIterator;
import openmods.calc.IExecutable;

public interface ICompiler<E> {
	public IExecutable<E> compile(PeekingIterator<Token> input);
}
