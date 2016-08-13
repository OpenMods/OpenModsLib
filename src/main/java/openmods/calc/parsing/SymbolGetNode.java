package openmods.calc.parsing;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.IExecutable;
import openmods.calc.SymbolGet;

public class SymbolGetNode<E> implements IExprNode<E> {

	private final String symbol;

	public SymbolGetNode(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public void flatten(List<IExecutable<E>> output) {
		output.add(new SymbolGet<E>(symbol));
	}

	@Override
	public Iterable<IExprNode<E>> getChildren() {
		return ImmutableList.of();
	}

	@Override
	public String toString() {
		return "<get: " + symbol + ">";
	}

	public String symbol() {
		return symbol;
	}
}
