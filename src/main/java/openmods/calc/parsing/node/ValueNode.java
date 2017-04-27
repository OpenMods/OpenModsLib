package openmods.calc.parsing.node;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.Value;

public class ValueNode<E> implements IExprNode<E> {

	public final E value;

	public ValueNode(E value) {
		this.value = value;
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		output.add(Value.create(value));
	}

	@Override
	public String toString() {
		return "<v: " + value + ">";
	}

	@Override
	public Iterable<IExprNode<E>> getChildren() {
		return ImmutableList.of();
	}
}
