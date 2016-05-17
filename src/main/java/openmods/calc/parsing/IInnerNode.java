package openmods.calc.parsing;

public interface IInnerNode<E> extends IExprNode<E> {
	public void addChild(IExprNode<E> child);
}
