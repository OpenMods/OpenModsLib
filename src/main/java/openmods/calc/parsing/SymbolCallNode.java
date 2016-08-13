package openmods.calc.parsing;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolCall;

public class SymbolCallNode<E> implements IExprNode<E> {

	private final String symbol;

	private final List<IExprNode<E>> args;

	public SymbolCallNode(String symbol, List<IExprNode<E>> args) {
		this.symbol = symbol;
		this.args = ImmutableList.copyOf(args);
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		for (IExprNode<E> arg : args)
			arg.flatten(output);

		output.add(new SymbolCall<E>(symbol, args.size(), 1));
	}

	@Override
	public String toString() {
		return "<call: " + symbol + " " + args + ">";
	}

	public String symbol() {
		return symbol;
	}

	@Override
	public Iterable<IExprNode<E>> getChildren() {
		return args;
	}
}
