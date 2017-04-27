package openmods.calc.parsing;

import com.google.common.collect.PeekingIterator;
import openmods.calc.executable.IExecutable;
import openmods.calc.parsing.token.Token;

public interface ITokenStreamCompiler<E> {
	public IExecutable<E> compile(PeekingIterator<Token> input);
}
