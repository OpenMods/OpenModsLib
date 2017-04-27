package openmods.calc.parsing.node;

import com.google.common.collect.ImmutableList;
import java.util.List;
import openmods.calc.executable.IExecutable;
import openmods.calc.executable.SymbolCall;

public class SymbolCallNode<E> extends SymbolOpNode<E> {

	private final List<IExprNode<E>> args;

	public SymbolCallNode(String symbol, List<? extends IExprNode<E>> args) {
		super(symbol);
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

	@Override
	public Iterable<IExprNode<E>> getChildren() {
		return args;
	}
}
