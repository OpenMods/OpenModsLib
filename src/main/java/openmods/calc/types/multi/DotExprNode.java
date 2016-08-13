package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.IExecutable;
import openmods.calc.Value;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SymbolGetNode;

class DotExprNode implements IExprNode<TypedValue> {
	private final IExprNode<TypedValue> rightChild;
	private final IExprNode<TypedValue> leftChild;
	private final BinaryOperator<TypedValue> dotOperator;
	private final TypeDomain domain;

	DotExprNode(IExprNode<TypedValue> rightChild, IExprNode<TypedValue> leftChild, BinaryOperator<TypedValue> dotOperator, TypeDomain domain) {
		this.rightChild = rightChild;
		this.leftChild = leftChild;
		this.dotOperator = dotOperator;
		this.domain = domain;
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		leftChild.flatten(output);
		if (rightChild instanceof SymbolGetNode) {
			final SymbolGetNode<TypedValue> symbolNode = (SymbolGetNode<TypedValue>)rightChild;
			output.add(Value.create(domain.create(String.class, symbolNode.symbol())));
		} else {
			rightChild.flatten(output);
		}
		output.add(dotOperator);
	}

	@Override
	public Iterable<IExprNode<TypedValue>> getChildren() {
		return ImmutableList.of(leftChild, rightChild);
	}
}