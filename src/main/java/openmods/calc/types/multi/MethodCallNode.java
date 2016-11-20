package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.Value;
import openmods.calc.parsing.IExprNode;

public class MethodCallNode implements IExprNode<TypedValue> {

	public final String symbol;
	public final IExprNode<TypedValue> target;
	public final IExprNode<TypedValue> args;

	public MethodCallNode(String symbol, IExprNode<TypedValue> target, IExprNode<TypedValue> args) {
		this.symbol = symbol;
		this.target = target;
		this.args = args;
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		target.flatten(output);
		flattenArgsAndSymbol(output);
	}

	protected void flattenArgsAndSymbol(List<IExecutable<TypedValue>> output) {
		int argCount = 0;
		for (IExprNode<TypedValue> arg : args.getChildren()) {
			arg.flatten(output);
			argCount++;
		}

		output.add(new SymbolCall<TypedValue>(symbol, argCount + 1, 1));
	}

	@Override
	public Iterable<IExprNode<TypedValue>> getChildren() {
		final ImmutableList.Builder<IExprNode<TypedValue>> builder = ImmutableList.builder();
		builder.add(target);
		builder.addAll(args.getChildren());
		return builder.build();
	}

	public static class NullAware extends MethodCallNode {

		private final TypeDomain domain;

		public NullAware(String symbol, IExprNode<TypedValue> target, IExprNode<TypedValue> args, TypeDomain domain) {
			super(symbol, target, args);
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
