package openmods.calc.parsing;

import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import java.util.List;
import openmods.calc.ExecutableList;
import openmods.calc.IExecutable;

public class AstCompiler<E> implements ICompiler<E> {

	private final IAstParserProvider<E> parserProvider;

	public AstCompiler(IAstParserProvider<E> parserProvider) {
		this.parserProvider = parserProvider;
	}

	@Override
	public IExecutable<E> compile(PeekingIterator<Token> input) {
		final IAstParser<E> parser = parserProvider.getParser();
		final IExprNode<E> rootNode = parser.parse(input);

		final List<IExecutable<E>> output = Lists.newArrayList();
		rootNode.flatten(output);
		return new ExecutableList<E>(output);
	}

}
