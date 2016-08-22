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
		flattenKeyNode(output, rightChild);
	}

	// algorithm: find leftmost node, find out if it's symbol, if yes, convert to string and then unwrap rest of triee

	private void flattenKeyNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> target) {
		if (target instanceof MethodCallNode) {
			flattenMethodCallNode(output, target); // recurse into left node
		} else {
			appendKey(output, target); // leftmost node, flatten and start unwrapping
			output.add(dotOperator);
		}
	}

	private void flattenMethodCallNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> node) {
		final MethodCallNode call = (MethodCallNode)node;
		flattenKeyNode(output, call.target); // recurse into left node
		call.flattenArgsAndCall(output); // unwrap rest
	}

	private void appendKey(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> target) {
		if (target instanceof SymbolGetNode) {
			convertSymbolToKey(output, target);
		} else {
			target.flatten(output);
		}
	}

	private void convertSymbolToKey(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> target) {
		final SymbolGetNode<TypedValue> symbolNode = (SymbolGetNode<TypedValue>)target;
		output.add(Value.create(domain.create(String.class, symbolNode.symbol())));
	}

	@Override
	public Iterable<IExprNode<TypedValue>> getChildren() {
		return ImmutableList.of(leftChild, rightChild);
	}
}