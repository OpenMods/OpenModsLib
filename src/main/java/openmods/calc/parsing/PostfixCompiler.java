package openmods.calc.parsing;

import com.google.common.collect.PeekingIterator;
import openmods.calc.executable.IExecutable;
import openmods.calc.parsing.postfix.PostfixParser;
import openmods.calc.parsing.token.Token;

public class PostfixCompiler<E> implements ITokenStreamCompiler<E> {

	private final PostfixParser<IExecutable<E>> parser;

	public PostfixCompiler(PostfixParser<IExecutable<E>> parser) {
		this.parser = parser;
	}

	@Override
	public IExecutable<E> compile(PeekingIterator<Token> input) {
		return parser.parse(input);
	}

}
