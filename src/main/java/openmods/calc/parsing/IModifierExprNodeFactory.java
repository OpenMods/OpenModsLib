package openmods.calc.parsing;

public interface IModifierExprNodeFactory<E> extends IExprNodeFactory<E> {
	public IExprNode<E> createRootModifierNode(IExprNode<E> child);
}
