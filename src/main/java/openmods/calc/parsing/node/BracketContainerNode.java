package openmods.calc.parsing.node;

import java.util.List;

public class BracketContainerNode<E> extends ContainerNode<E> {

	public final String openingBracket;

	public final String closingBracket;

	public BracketContainerNode(List<IExprNode<E>> args, String openingBracket, String closingBracket) {
		super(args);
		this.openingBracket = openingBracket;
		this.closingBracket = closingBracket;
	}

}
