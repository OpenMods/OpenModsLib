package openmods.calc.parsing.node;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.UnaryOperator;

public class UnaryOpNode<E> implements IExprNode<E> {

	public final UnaryOperator<E> operator;

	public final IExprNode<E> argument;

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
	public Iterable<IExprNode<E>> getChildren() {
		return ImmutableList.of(argument);
	}
}
