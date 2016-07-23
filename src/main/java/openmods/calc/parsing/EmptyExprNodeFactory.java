package openmods.calc.parsing;

import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.UnaryOperator;

public class EmptyExprNodeFactory<E> implements IExprNodeFactory<E> {

	@Override
	public IExprNode<E> createSymbolNode(String value, List<IExprNode<E>> children) {
		throw new UnsupportedOperationException("Symbol: " + value);
	}

	@Override
	public IExprNode<E> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<E>> children) {
		throw new UnsupportedOperationException("Bracket: " + openingBracket + closingBracket);
	}

	@Override
	public IExprNode<E> createBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild) {
		throw new UnsupportedOperationException("Binary op: " + op);
	}

	@Override
	public IExprNode<E> createUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child) {
		throw new UnsupportedOperationException("Unary op: " + op);
	}

	@Override
	public IExprNode<E> createValueNode(E value) {
		throw new UnsupportedOperationException("Value: " + value);
	}

	@Override
	public IExprNode<E> createRootNode(IExprNode<E> child) {
		throw new UnsupportedOperationException("Root: " + child);
	}

	@Override
	public IExprNode<E> createRawValueNode(Token token) {
		throw new UnsupportedOperationException("Raw: " + token);
	}

	@Override
	public IExprNodeFactory<E> getExprNodeFactoryForModifier(String modifier) {
		throw new UnsupportedOperationException("Modifier: " + modifier);
	}

}
