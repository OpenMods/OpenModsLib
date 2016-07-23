package openmods.calc.parsing;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;

public class NullNode<E> implements IExprNode<E> {

	@Override
	public void flatten(List<IExecutable<E>> output) {}

	@Override
	public Iterable<IExprNode<E>> getChildren() {
		return ImmutableList.of();
	}

}
