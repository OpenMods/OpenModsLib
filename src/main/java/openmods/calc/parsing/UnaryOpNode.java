package openmods.calc.parsing;

import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.UnaryOperator;

public class UnaryOpNode<E> implements IExprNode<E> {

	private final UnaryOperator<E> operator;

	private final IExprNode<E> argument;

	public UnaryOpNode(UnaryOperator<E> operator, IExprNode<E> argument) {
		this.operator = operator;
		this.argument = argument;
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		argument.flatten(output);
		output.add(operator);
	}

	@Override
	public String toString() {
		return "<op: " + operator.id + " a: " + argument + ">";
	}

	@Override
	public int numberOfChildren() {
		return 1;
	}
}
