package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SquareBracketContainerNode;
import openmods.calc.parsing.SymbolCallNode;

public abstract class ScopeModifierNode extends SymbolCallNode<TypedValue> {

	private final TypeDomain domain;
	private final String symbol;
	private final BinaryOperator<TypedValue> colonOperator;
	private final BinaryOperator<TypedValue> assignOperator;

	public ScopeModifierNode(TypeDomain domain, String symbol, BinaryOperator<TypedValue> colonOperator, BinaryOperator<TypedValue> assignOperator, List<IExprNode<TypedValue>> args) {
		super(symbol, args);
		this.domain = domain;
		this.symbol = symbol;
		this.colonOperator = colonOperator;
		this.assignOperator = assignOperator;

	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		final List<IExprNode<TypedValue>> args = ImmutableList.copyOf(getChildren());

		Preconditions.checkState(args.size() == 2, "Expected two args for '%s' expression", symbol);
		final IExprNode<TypedValue> argsNode = args.get(0);
		final IExprNode<TypedValue> codeNode = args.get(1);

		// expecting [a:...,c:...]. Arg name (symbol) will be transformed into symbol atom
		Preconditions.checkState(argsNode instanceof SquareBracketContainerNode, "Expected square brackets, got %s", argsNode);
		final SquareBracketContainerNode<TypedValue> bracketNode = (SquareBracketContainerNode<TypedValue>)argsNode;

		int argumentCount = 0;
		for (IExprNode<TypedValue> argNode : bracketNode.getChildren()) {
			flattenArgNode(output, argNode);
			argumentCount++;
		}

		Preconditions.checkState(argumentCount > 0, "'%s' expects at least one argument", symbol);
		// slighly inefficient, but compatible with hand-called instruction
		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_LIST, argumentCount, 1));

		output.add(Value.create(Code.flattenAndWrap(domain, codeNode)));
		output.add(new SymbolCall<TypedValue>(symbol, 2, 1));
	}

	private void flattenArgNode(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> argNode) {
		Preconditions.checkState(argNode instanceof BinaryOpNode, "Expected ':' or '=' as separator");
		final BinaryOpNode<TypedValue> opNode = (BinaryOpNode<TypedValue>)argNode;
		// assign op has lower prio, but we still need pair as backing structure - therefore we are placing colon
		if (opNode.operator == colonOperator || opNode.operator == assignOperator) {
			flattenNameAndValue(output, opNode.left, opNode.right);
		} else {
			handlePairOp(output, opNode);
		}
		output.add(colonOperator);
	}

	protected void handlePairOp(List<IExecutable<TypedValue>> output, BinaryOpNode<TypedValue> opNode) {
		throw new UnsupportedOperationException("Expected '=' or ':' as pair separators, got " + opNode.operator);
	}

	protected abstract void flattenNameAndValue(List<IExecutable<TypedValue>> output, IExprNode<TypedValue> name, IExprNode<TypedValue> value);
}
