package openmods.calc.parsing;

import java.util.List;

public interface ISymbolCallStateTransition<E> {
	public ICompilerState<E> getState();

	public IExprNode<E> createRootNode(List<IExprNode<E>> children);
}