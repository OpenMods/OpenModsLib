package openmods.calc.parsing;

public abstract class SymbolOpNode<E> implements IExprNode<E> {

	protected final String symbol;

	public SymbolOpNode(String symbol) {
		this.symbol = symbol;
	}

	public String symbol() {
		return symbol;
	}
}
