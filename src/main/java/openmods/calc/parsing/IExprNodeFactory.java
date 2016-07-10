package openmods.calc.parsing;

import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.UnaryOperator;

public interface IExprNodeFactory<E> {

	public IExprNode<E> createSymbolNode(String value, List<IExprNode<E>> children);

	public IExprNode<E> createBracketNode(String openingBracket, List<IExprNode<E>> children);

	public IExprNode<E> createBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild);

	public IExprNode<E> createUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child);

}
