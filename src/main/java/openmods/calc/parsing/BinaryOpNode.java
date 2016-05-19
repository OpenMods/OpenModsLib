package openmods.calc.parsing;

import java.util.List;

import openmods.calc.BinaryOperator;
import openmods.calc.IExecutable;

public class BinaryOpNode<E> implements IExprNode<E> {

	private final BinaryOperator<E> operator;

	private final IExprNode<E> left;

	private final IExprNode<E> right;

	public BinaryOpNode(BinaryOperator<E> operator, IExprNode<E> left, IExprNode<E> right) {
		this.operator = operator;
		this.left = left;
		this.right = right;
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		left.flatten(output);
		right.flatten(output);
		output.add(operator);
	}

	@Override
	public String toString() {
		return "<op: '" + operator.id + "', l: " + left + ", r: " + right + ">";
	}
}
