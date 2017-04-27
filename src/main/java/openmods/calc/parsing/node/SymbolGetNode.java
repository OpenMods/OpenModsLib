package openmods.calc.parsing.node;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.SymbolGet;

public class SymbolGetNode<E> extends SymbolOpNode<E> {

	public SymbolGetNode(String symbol) {
		super(symbol);
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
}
