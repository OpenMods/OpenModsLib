package openmods.calc.parsing;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;

public class BracketNode<E> implements IExprNode<E> {

	private final IExprNode<E> child;

	public BracketNode(IExprNode<E> child) {
		this.child = child;
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		if (child != null) child.flatten(output);
	}

	@Override
	public String toString() {
		return "<" + child + ">";
	}

	@Override
	public Iterable<IExprNode<E>> getChildren() {
		return ImmutableList.of(child);
	}
}
