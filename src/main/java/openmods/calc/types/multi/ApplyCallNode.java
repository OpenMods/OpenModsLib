package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.UnaryOperator;
import openmods.calc.Value;
import openmods.calc.parsing.IExprNode;

public class ApplyCallNode implements IExprNode<TypedValue> {

	public final IExprNode<TypedValue> target;
	public final IExprNode<TypedValue> args;
	private final UnaryOperator<TypedValue> unpackMarker;

	public ApplyCallNode(UnaryOperator<TypedValue> unpackMarker, IExprNode<TypedValue> target, IExprNode<TypedValue> args) {
		this.target = target;
		this.args = args;
		this.unpackMarker = unpackMarker;
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		target.flatten(output);
		flattenArgsAndSymbol(output);
	}

	protected void flattenArgsAndSymbol(final List<IExecutable<TypedValue>> output) {
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
		}.compileArgUnpack(args.getChildren());
	}

	@Override
	public Iterable<IExprNode<TypedValue>> getChildren() {
		final ImmutableList.Builder<IExprNode<TypedValue>> builder = ImmutableList.builder();
		builder.add(target);
		builder.addAll(args.getChildren());
		return builder.build();
	}

	public static class NullAware extends ApplyCallNode {

		private final TypeDomain domain;

		public NullAware(UnaryOperator<TypedValue> unpackMarker, IExprNode<TypedValue> target, IExprNode<TypedValue> args, TypeDomain domain) {
			super(unpackMarker, target, args);
			this.domain = domain;
		}

		@Override
		public void flatten(List<IExecutable<TypedValue>> output) {
			target.flatten(output);

			final List<IExecutable<TypedValue>> nonNullOp = Lists.newArrayList();
			flattenArgsAndSymbol(nonNullOp);

			output.add(Value.create(Code.wrap(domain, nonNullOp)));

			output.add(new SymbolCall<TypedValue>(TypedCalcConstants.SYMBOL_NULL_EXECUTE, 2, 1));
		}

	}
}
