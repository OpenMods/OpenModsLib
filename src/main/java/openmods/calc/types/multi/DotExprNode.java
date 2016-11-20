package openmods.calc.types.multi;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.SymbolCallNode;
import openmods.calc.parsing.SymbolGetNode;
import openmods.calc.parsing.SymbolOpNode;

public class DotExprNode extends BinaryOpNode<TypedValue> {

	protected final TypeDomain domain;

	public DotExprNode(IExprNode<TypedValue> left, IExprNode<TypedValue> right, BinaryOperator<TypedValue> operator, TypeDomain domain) {
		super(operator, left, right);
		this.domain = domain;
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		left.flatten(output);
		flattenKeyNode(output);
	}

	// tring to convert symbol to key string (to be placed on the right side of dot)
	private void flattenKeyNode(List<IExecutable<TypedValue>> output) {
		if (right instanceof SymbolCallNode) { // symbol call -> use dot to extract member and apply args
			flattenMemberApplyNode(output);
		} else if (right instanceof SymbolGetNode) { // symbol get -> convert to string, use with dot
			flattenMemberGet(output);
		} else if (right instanceof RawCodeExprNode) { // with (.{...}) statement
			flattenWithNode(output);
		} else { // terminal node - anything else (possibly something that returns string)
			flattenNonTrivialMemberGetNode(output);
		}
	}

	protected void flattenMemberGet(List<IExecutable<TypedValue>> output) {
		convertSymbolNodeToKey(output, (SymbolGetNode<TypedValue>)right);
		output.add(operator);
	}

	protected void flattenNonTrivialMemberGetNode(List<IExecutable<TypedValue>> output) {
		right.flatten(output);
		output.add(operator);
	}

	protected void flattenMemberApplyNode(List<IExecutable<TypedValue>> output) {
		final SymbolCallNode<TypedValue> call = (SymbolCallNode<TypedValue>)right;
		convertSymbolNodeToKey(output, call);
		output.add(operator);
		appendSymbolApply(output, call.getChildren());
	}

	protected void flattenWithNode(List<IExecutable<TypedValue>> output) {
		right.flatten(output);
		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_WITH, 2, 1));
	}

	private static void appendSymbolApply(List<IExecutable<TypedValue>> output, Iterable<IExprNode<TypedValue>> children) {
		int applyArgs = 1;
		for (IExprNode<TypedValue> child : children) {
			child.flatten(output);
			applyArgs++;
		}

		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_APPLY, applyArgs, 1));
	}

	private void convertSymbolNodeToKey(List<IExecutable<TypedValue>> output, SymbolOpNode<TypedValue> target) {
		output.add(Value.create(domain.create(String.class, target.symbol())));
	}

	public static class NullAware extends DotExprNode {

		public NullAware(IExprNode<TypedValue> left, IExprNode<TypedValue> right, BinaryOperator<TypedValue> operator, TypeDomain domain) {
			super(left, right, operator, domain);
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			left.flatten(output);

			final List<IExecutable<TypedValue>> nonNullOp = Lists.newArrayList();
			flattenKeyNode(nonNullOp);

			output.add(Value.create(Code.wrap(domain, nonNullOp)));

			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_NULL_EXECUTE, 2, 1));
		}

		private void flattenKeyNode(List<IExecutable<TypedValue>> output) {
			if (right instanceof SymbolGetNode) { // symbol get -> convert to string, use with dot
				flattenMemberGet(output);
			} else if (right instanceof RawCodeExprNode) { // with (.{...}) statement
				flattenWithNode(output);
			} else { // terminal node - anything else (possibly something that returns string)
				flattenNonTrivialMemberGetNode(output);
			}

			// if (right instanceof SymbolCallNode) <- disabled, for symmetry with [...], use .?member?()
		}
	}
}