package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SymbolCallNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.calc.parsing.SymbolOpNode;

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

	// algorithm: if node has children, recurse, otherwise try to extract key (to be placed on the right side of dot)
	private void flattenKeyNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> target) {
		if (target instanceof MethodCallNode) { // non-terminal: "left" node has children
			final MethodCallNode call = (MethodCallNode)target;
			flattenKeyNode(output, call.target); // recurse into left node
			call.flattenArgsAndCall(output); // unwrap rest
		} else if (target instanceof SymbolCallNode) { // terminal node: symbol call
			final SymbolCallNode<TypedValue> call = (SymbolCallNode<TypedValue>)target;
			convertSymbolNodeToKey(output, call);
			output.add(dotOperator);
			appendSymbolApply(output, call.getChildren());
		} else if (target instanceof SymbolGetNode) { // terminal node: symbol get - convert to string
			convertSymbolNodeToKey(output, (SymbolGetNode<TypedValue>)target);
			output.add(dotOperator);
		} else { // terminal node - anything else (possible something that returns string)
			target.flatten(output);
			output.add(dotOperator);
		}
	}

	private static void appendSymbolApply(List<IExecutable<TypedValue>> output, Iterable<IExprNode<TypedValue>> children) {
		int applyArgs = 1;
		for (IExprNode<TypedValue> child : children) {
			child.flatten(output);
			applyArgs++;
		}

		output.add(new SymbolCall<TypedValue>(TypedValueCalculatorFactory.SYMBOL_APPLY, applyArgs, 1));
	}

	private void convertSymbolNodeToKey(List<IExecutable<TypedValue>> output, SymbolOpNode<TypedValue> target) {
		output.add(Value.create(domain.create(String.class, target.symbol())));
	}

	@Override
	public Iterable<IExprNode<TypedValue>> getChildren() {
		return ImmutableList.of(leftChild, rightChild);
	}
}