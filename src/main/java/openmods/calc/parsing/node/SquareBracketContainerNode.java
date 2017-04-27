package openmods.calc.parsing.node;

import java.util.List;
import openmods.calc.parsing.node.MappedExprNodeFactory.IBracketExprNodeFactory;

public class SquareBracketContainerNode<E> extends BracketContainerNode<E> {
	public static final String BRACKET_OPEN = "[";

	public static final String BRACKET_CLOSE = "]";

	public SquareBracketContainerNode(List<IExprNode<E>> args) {
		super(args, BRACKET_OPEN, BRACKET_CLOSE);
	}

	public static <E> IBracketExprNodeFactory<E> createNodeFactory() {
		return new IBracketExprNodeFactory<E>() {
			@Override
			public IExprNode<E> create(List<IExprNode<E>> children) {
				return new SquareBracketContainerNode<E>(children);
			}
		};
	}

	public static <E, F extends MappedExprNodeFactory<E>> F install(F nodeFactory) {
		nodeFactory.addFactory(BRACKET_OPEN, SquareBracketContainerNode.<E> createNodeFactory());
		return nodeFactory;
	}
}