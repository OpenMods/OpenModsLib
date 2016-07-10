package openmods.calc.parsing;

import openmods.calc.BinaryOperator;
import openmods.calc.UnaryOperator;

public class DefaultExprNodeFactory<E> implements IExprNodeFactory<E> {

	@Override
	public IInnerNode<E> createSymbolNode(String value) {
		return new SymbolNode<E>(value);
	}

	@Override
	public IInnerNode<E> createBracketNode(String openingBracket) {
		return new NullNode<E>();
	}

	@Override
	public IExprNode<E> createBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild) {
		return new BinaryOpNode<E>(op, leftChild, rightChild);
	}

	@Override
	public IExprNode<E> createUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child) {
		return new UnaryOpNode<E>(op, child);
	}

}
