package openmods.calc.parsing.node;

import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.executable.BinaryOperator;
import openmods.calc.executable.Operator;
import openmods.calc.executable.UnaryOperator;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.ast.INodeFactory;
import openmods.calc.parsing.token.Token;
import openmods.calc.parsing.token.TokenUtils;

public class DefaultExprNodeFactory<E> implements INodeFactory<IExprNode<E>, Operator<E>> {

	private final IValueParser<E> valueParser;

	public DefaultExprNodeFactory(IValueParser<E> valueParser) {
		this.valueParser = valueParser;
	}

	@Override
	public IExprNode<E> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<E>> children) {
		TokenUtils.checkIsValidBracketPair(openingBracket, closingBracket);
		Preconditions.checkState(children.size() == 1, "Invalid number of children for bracket node: %s", children);
		return new BracketNode<E>(children.iterator().next());
	}

	@Override
	public IExprNode<E> createOpNode(Operator<E> op, List<IExprNode<E>> children) {
		Preconditions.checkArgument(op.arity().args == children.size(), "Got %s children for operator %s with arity %s", children, op, op.arity());
		// TODO maybe merge binary and unary op nodes?
		if (op instanceof BinaryOperator)
			return new BinaryOpNode<E>((BinaryOperator<E>)op, children.get(0), children.get(1));

		if (op instanceof UnaryOperator)
			return new UnaryOpNode<E>((UnaryOperator<E>)op, children.get(0));

		throw new IllegalArgumentException("Unsupported arity " + op.arity() + " on operator " + op);
	}

	@Override
	public IExprNode<E> createValueNode(Token token) {
		return new ValueNode<E>(valueParser.parseToken(token));
	}

	@Override
	public IExprNode<E> createSymbolGetNode(String id) {
		return new SymbolGetNode<E>(id);
	}

}
