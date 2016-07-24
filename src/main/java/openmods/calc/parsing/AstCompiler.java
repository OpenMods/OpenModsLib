package openmods.calc.parsing;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.ExecutableList;
import openmods.calc.IExecutable;

public abstract class AstCompiler<E> implements ICompiler<E> {

	private IExprNodeFactory<E> rootExprNodeFactory;

	public AstCompiler(IExprNodeFactory<E> rootExprNodeFactory) {
		this.rootExprNodeFactory = rootExprNodeFactory;
	}

	@Override
	public IExecutable<E> compile(Iterable<Token> input) {
		final IExprNode<E> result = compileAst(input, rootExprNodeFactory);

		final List<IExecutable<E>> output = Lists.newArrayList();
		result.flatten(output);
		return new ExecutableList<E>(output);
	}

	protected abstract IExprNode<E> compileAst(Iterable<Token> input, IExprNodeFactory<E> exprNodeFactory);

}
