package openmods.calc.parsing.node;

import java.util.List;
import openmods.calc.executable.IExecutable;

public interface IExprNode<E> {

	public void flatten(List<IExecutable<E>> output);

	public Iterable<IExprNode<E>> getChildren();
}
