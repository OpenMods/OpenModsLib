package openmods.calc.parsing;

import java.util.List;
import openmods.calc.BinaryOperator;
import openmods.calc.UnaryOperator;

public class DelegateExprNodeFactory<E> implements IExprNodeFactory<E> {

	private final IExprNodeFactory<E> wrapped;

	public DelegateExprNodeFactory(IExprNodeFactory<E> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public IAstParser<E> getParser() {
		return wrapped.getParser();
	}

	@Override
	public ISymbolExprNodeFactory<E> createSymbolExprNodeFactory(String symbol) {
		return wrapped.createSymbolExprNodeFactory(symbol);
	}

	@Override
	public IExprNode<E> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<E>> children) {
		return wrapped.createBracketNode(openingBracket, closingBracket, children);
	}

	@Override
	public IExprNode<E> createBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild) {
		return wrapped.createBinaryOpNode(op, leftChild, rightChild);
	}

	@Override
	public IExprNode<E> createUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child) {
		return wrapped.createUnaryOpNode(op, child);
	}

	@Override
	public IExprNode<E> createValueNode(E value) {
		return wrapped.createValueNode(value);
	}

	@Override
	public IExprNode<E> createRawValueNode(Token token) {
		return wrapped.createRawValueNode(token);
	}

	@Override
	public IModifierExprNodeFactory<E> createModifierExprNodeFactory(String modifier) {
		return wrapped.createModifierExprNodeFactory(modifier);
	}

}
