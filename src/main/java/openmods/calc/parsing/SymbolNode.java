package openmods.calc.parsing;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolReference;

public class SymbolNode<E> implements IInnerNode<E> {

	private final String symbol;

	private final List<IExprNode<E>> children = Lists.newArrayList();

	public SymbolNode(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public void addChild(IExprNode<E> child) {
		children.add(child);
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		for (IExprNode<E> e : children)
			e.flatten(output);

		final SymbolReference<E> symbolRef =
				new SymbolReference<E>(symbol)
						.setArgumentsCount(children.size())
						.setReturnsCount(1);
		output.add(symbolRef);
	}

	@Override
	public String toString() {
		return "<s: " + symbol + " " + children + ">";
	}
}
