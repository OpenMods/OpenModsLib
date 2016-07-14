package openmods.calc.parsing;

import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.UnaryOperator;

public class DefaultExprNodeFactory<E> implements IExprNodeFactory<E> {

	@Override
	public IExprNode<E> createSymbolNode(String value, List<IExprNode<E>> args) {
		return new SymbolNode<E>(value, args);
	}

	@Override
	public IExprNode<E> createBracketNode(String openingBracket, List<IExprNode<E>> children) {
		Preconditions.checkState(children.size() == 1, "Invalid number of children for bracket node: %s", children);
		return new BracketNode<E>(children.iterator().next());
	}

	@Override
	public IExprNode<E> createBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild) {
		return new BinaryOpNode<E>(op, leftChild, rightChild);
	}

	@Override
	public IExprNode<E> createUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child) {
		return new UnaryOpNode<E>(op, child);
	}

	@Override
	public IExprNode<E> createValueNode(E value) {
		return new ValueNode<E>(value);
	}

}
