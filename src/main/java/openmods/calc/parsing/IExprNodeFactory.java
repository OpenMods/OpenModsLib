package openmods.calc.parsing;

import openmods.calc.BinaryOperator;
import openmods.calc.UnaryOperator;

public interface IExprNodeFactory<E> {

	public IInnerNode<E> createSymbolNode(String value);

	public IInnerNode<E> createBracketNode(String openingBracket);

	public IExprNode<E> createBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild);

	public IExprNode<E> createUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child);

}
