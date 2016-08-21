package openmods.calc.parsing;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import openmods.calc.BinaryOperator;
import openmods.calc.UnaryOperator;
import openmods.utils.CollectionUtils;

public class MappedExprNodeFactory<E> extends DefaultExprNodeFactory<E> {

	public MappedExprNodeFactory(IValueParser<E> valueParser) {
		super(valueParser);
	}

	public interface IBracketExprNodeFactory<E> {
		public IExprNode<E> create(List<IExprNode<E>> children);
	}

	public interface IBinaryExprNodeFactory<E> {
		public IExprNode<E> create(IExprNode<E> leftChild, IExprNode<E> rightChild);
	}

	public interface IUnaryExprNodeFactory<E> {
		public IExprNode<E> create(IExprNode<E> child);
	}

	private final Map<String, IBracketExprNodeFactory<E>> bracketFactories = Maps.newHashMap();
	private final Map<BinaryOperator<E>, IBinaryExprNodeFactory<E>> binaryOpFactories = Maps.newIdentityHashMap();
	private final Map<UnaryOperator<E>, IUnaryExprNodeFactory<E>> unaryOpFactories = Maps.newIdentityHashMap();

	@Override
	public IExprNode<E> createBracketNode(String openingBracket, String closingBracket, List<IExprNode<E>> children) {
		TokenUtils.checkIsValidBracketPair(openingBracket, closingBracket);
		final IBracketExprNodeFactory<E> nodeFactory = bracketFactories.get(openingBracket);
		return nodeFactory != null? nodeFactory.create(children) : createDefaultBracketNode(openingBracket, closingBracket, children);
	}

	protected IExprNode<E> createDefaultBracketNode(String openingBracket, String closingBracket, List<IExprNode<E>> children) {
		return super.createBracketNode(openingBracket, closingBracket, children);
	}

	@Override
	public IExprNode<E> createBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild) {
		final IBinaryExprNodeFactory<E> nodeFactory = binaryOpFactories.get(op);
		return nodeFactory != null? nodeFactory.create(leftChild, rightChild) : createDefaultBinaryOpNode(op, leftChild, rightChild);
	}

	protected IExprNode<E> createDefaultBinaryOpNode(BinaryOperator<E> op, IExprNode<E> leftChild, IExprNode<E> rightChild) {
		return super.createBinaryOpNode(op, leftChild, rightChild);
	}

	@Override
	public IExprNode<E> createUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child) {
		final IUnaryExprNodeFactory<E> nodeFactory = unaryOpFactories.get(op);
		return nodeFactory != null? nodeFactory.create(child) : createDefaultUnaryOpNode(op, child);
	}

	protected IExprNode<E> createDefaultUnaryOpNode(UnaryOperator<E> op, IExprNode<E> child) {
		return super.createUnaryOpNode(op, child);
	}

	public MappedExprNodeFactory<E> addFactory(String openingBracket, IBracketExprNodeFactory<E> factory) {
		CollectionUtils.putOnce(bracketFactories, openingBracket, factory);
		return this;
	}

	public MappedExprNodeFactory<E> addFactory(BinaryOperator<E> op, IBinaryExprNodeFactory<E> factory) {
		CollectionUtils.putOnce(binaryOpFactories, op, factory);
		return this;
	}

	public MappedExprNodeFactory<E> addFactory(UnaryOperator<E> op, IUnaryExprNodeFactory<E> factory) {
		CollectionUtils.putOnce(unaryOpFactories, op, factory);
		return this;
	}

}
