package openmods.calc.parsing.node;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.executable.IExecutable;

public class DummyNode<E> implements IExprNode<E> {

	private final IExprNode<E> child;

	public DummyNode(IExprNode<E> child) {
		this.child = child;
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		child.flatten(output);
	}

	@Override
	public Iterable<IExprNode<E>> getChildren() {
		return ImmutableList.of(child);
	}

}
