package openmods.calc.parsing;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolReference;

public class SymbolNode<E> implements IExprNode<E> {

	private final String symbol;

	private final List<IExprNode<E>> args;

	public SymbolNode(String symbol, List<IExprNode<E>> args) {
		this.symbol = symbol;
		this.args = ImmutableList.copyOf(args);
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		for (IExprNode<E> arg : args)
			arg.flatten(output);

		final SymbolReference<E> symbolRef =
				new SymbolReference<E>(symbol)
						.setArgumentsCount(args.size())
						.setReturnsCount(1);
		output.add(symbolRef);
	}

	@Override
	public String toString() {
		return "<s: " + symbol + " " + args + ">";
	}

	public String symbol() {
		return symbol;
	}

	@Override
	public int numberOfChildren() {
		return args.size();
	}
}
