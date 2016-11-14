package openmods.calc.types.multi;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;
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

}
