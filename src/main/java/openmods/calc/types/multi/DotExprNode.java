package openmods.calc.types.multi;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.executable.BinaryOperator;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.SymbolCall;
import openmods.calc.executable.UnaryOperator;
import openmods.calc.executable.Value;
import openmods.calc.parsing.node.BinaryOpNode;
import openmods.calc.parsing.node.IExprNode;
import openmods.calc.parsing.node.SymbolCallNode;
import openmods.calc.parsing.node.SymbolGetNode;
import openmods.calc.parsing.node.SymbolOpNode;

public abstract class DotExprNode extends BinaryOpNode<TypedValue> {

	protected final TypeDomain domain;

	public DotExprNode(IExprNode<TypedValue> left, IExprNode<TypedValue> right, BinaryOperator<TypedValue> operator, TypeDomain domain) {
		super(operator, left, right);
		this.domain = domain;
	}

	protected void flattenMemberGet(List<IExecutable<TypedValue>> output) {
		convertSymbolNodeToKey(output, (SymbolGetNode<TypedValue>)right);
		output.add(operator);
	}

	protected void flattenNonTrivialMemberGetNode(List<IExecutable<TypedValue>> output) {
		right.flatten(output);
		output.add(operator);
	}

	protected void flattenWithNode(List<IExecutable<TypedValue>> output) {
		right.flatten(output);
		output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_WITH, 2, 1));
	}

	protected void convertSymbolNodeToKey(List<IExecutable<TypedValue>> output, SymbolOpNode<TypedValue> target) {
		output.add(Value.create(domain.create(String.class, target.symbol())));
	}

	public static class Basic extends DotExprNode {

		private final UnaryOperator<TypedValue> unpackMarker;

		public Basic(UnaryOperator<TypedValue> unpackMarker, IExprNode<TypedValue> left, IExprNode<TypedValue> right, BinaryOperator<TypedValue> operator, TypeDomain domain) {
			super(left, right, operator, domain);
			this.unpackMarker = unpackMarker;
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

		protected void flattenMemberApplyNode(List<IExecutable<TypedValue>> output) {
			final SymbolCallNode<TypedValue> call = (SymbolCallNode<TypedValue>)right;
			convertSymbolNodeToKey(output, call);
			output.add(operator);
			appendSymbolApply(output, call.getChildren());
		}

		private void appendSymbolApply(final List<IExecutable<TypedValue>> output, Iterable<IExprNode<TypedValue>> children) {
			new ArgUnpackCompilerHelper(unpackMarker) {
				@Override
				protected void compileWithVarArgs(int normalArgCount, List<IExecutable<TypedValue>> compiledArgs) {
					output.addAll(compiledArgs);
					// normalArgCount+2 == len(target|*args|varArg list)
					output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_APPLYVAR, normalArgCount + 2, 1));
				}

				@Override
				protected void compileWithoutVarArgs(int allArgs, List<IExecutable<TypedValue>> compiledArgs) {
					output.addAll(compiledArgs);
					// allArgs+1 == len(target|*args|)
					output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_APPLY, allArgs + 1, 1));
				}
			}.compileArgUnpack(children);
		}
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
			if (right instanceof SymbolCallNode) { // disabled, for symmetry with [...], use .?member?(), which is more correct anyway
				throw new IllegalStateException("Can't call member with ?., use .?member?()");
			}
			if (right instanceof SymbolGetNode) { // symbol get -> convert to string, use with dot
				flattenMemberGet(output);
			} else if (right instanceof RawCodeExprNode) { // with (.{...}) statement
				flattenWithNode(output);
			} else { // terminal node - anything else (possibly something that returns string)
				flattenNonTrivialMemberGetNode(output);
			}

		}
	}
}