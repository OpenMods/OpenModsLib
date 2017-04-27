package openmods.calc.parsing.node;

public abstract class SymbolOpNode<E> implements IExprNode<E> {

	protected final String symbol;

	public SymbolOpNode(String symbol) {
		this.symbol = symbol;
	}

	public String symbol() {
		return symbol;
	}
}
