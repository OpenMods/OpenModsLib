package openmods.calc.parsing;

import java.util.List;

import openmods.calc.IExecutable;
import openmods.calc.Value;

public class ValueNode<E> implements IExprNode<E> {

	private final E value;

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
}
