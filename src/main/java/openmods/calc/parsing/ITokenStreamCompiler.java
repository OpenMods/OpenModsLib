package openmods.calc.parsing;

import com.google.common.collect.PeekingIterator;
import openmods.calc.IExecutable;

public interface ITokenStreamCompiler<E> {
	public IExecutable<E> compile(PeekingIterator<Token> input);
}
