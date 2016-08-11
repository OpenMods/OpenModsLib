package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.IExecutable;
import openmods.calc.Value;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SymbolNode;

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
		if (rightChild instanceof SymbolNode) {
			final SymbolNode<TypedValue> symbolNode = (SymbolNode<TypedValue>)rightChild;
			Preconditions.checkState(Iterables.isEmpty(symbolNode.getChildren())); // temporary
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