package openmods.calc.parsing;

import java.util.List;
import openmods.calc.IExecutable;

public interface IExprNode<E> {

	public void flatten(List<IExecutable<E>> output);

	public Iterable<IExprNode<E>> getChildren();
}
