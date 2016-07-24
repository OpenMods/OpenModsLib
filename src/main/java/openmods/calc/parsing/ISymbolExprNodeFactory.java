package openmods.calc.parsing;

import java.util.List;

public interface ISymbolExprNodeFactory<E> extends IExprNodeFactory<E> {
	public IExprNode<E> createRootSymbolNode(List<IExprNode<E>> children);

}
