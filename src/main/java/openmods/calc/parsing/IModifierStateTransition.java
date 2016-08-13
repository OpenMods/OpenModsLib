package openmods.calc.parsing;

public interface IModifierStateTransition<E> {
	public ICompilerState<E> getState();

	public IExprNode<E> createRootNode(IExprNode<E> child);
}