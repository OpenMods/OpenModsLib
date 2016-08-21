package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
import openmods.calc.parsing.IExprNode;

public class ApplyExprNode implements IExprNode<TypedValue> {

	private final IExprNode<TypedValue> callable;
	private final ArgBracketNode args;

	public ApplyExprNode(IExprNode<TypedValue> callable, ArgBracketNode args) {
		this.callable = callable;
		this.args = args;
	}

	@Override
	public void flatten(List<IExecutable<TypedValue>> output) {
		callable.flatten(output);
		int argCount = 0;
		for (IExprNode<TypedValue> arg : args.getChildren()) {
			arg.flatten(output);
			argCount++;
		}

		output.add(new SymbolCall<TypedValue>(TypedValueCalculatorFactory.SYMBOL_APPLY, argCount + 1, 1));
	}

	@Override
	public Iterable<IExprNode<TypedValue>> getChildren() {
		final ImmutableList.Builder<IExprNode<TypedValue>> builder = ImmutableList.builder();
		builder.add(callable);
		builder.addAll(args.getChildren());
		return builder.build();
	}

}
