package openmods.calc.parsing.node;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import openmods.calc.executable.BinaryOperator;
import openmods.calc.executable.Operator;
import openmods.calc.executable.UnaryOperator;
import openmods.calc.parsing.IValueParser;
import openmods.calc.parsing.token.TokenUtils;
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

	public interface IOpNodeFactory<E> {
		public IExprNode<E> create(List<IExprNode<E>> children);
	}

	private final Map<String, IBracketExprNodeFactory<E>> bracketFactories = Maps.newHashMap();

	private final Map<Operator<E>, IOpNodeFactory<E>> opFactories = Maps.newIdentityHashMap();

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
	public IExprNode<E> createOpNode(Operator<E> op, List<IExprNode<E>> children) {
		final IOpNodeFactory<E> nodeFactory = opFactories.get(op);
		return nodeFactory != null? nodeFactory.create(children) : createDefaultOpNode(op, children);
	}

	protected IExprNode<E> createDefaultOpNode(Operator<E> op, List<IExprNode<E>> children) {
		return super.createOpNode(op, children);
	}

	public MappedExprNodeFactory<E> addFactory(String openingBracket, IBracketExprNodeFactory<E> factory) {
		CollectionUtils.putOnce(bracketFactories, openingBracket, factory);
		return this;
	}

	public MappedExprNodeFactory<E> addFactory(BinaryOperator<E> op, final IBinaryExprNodeFactory<E> factory) {
		CollectionUtils.putOnce(opFactories, op, new IOpNodeFactory<E>() {
			@Override
			public IExprNode<E> create(List<IExprNode<E>> children) {
				Preconditions.checkArgument(children.size() == 2, "Expected 2 children, got %s", children);
				return factory.create(children.get(0), children.get(1));
			}
		});
		return this;
	}

	public MappedExprNodeFactory<E> addFactory(UnaryOperator<E> op, final IUnaryExprNodeFactory<E> factory) {
		CollectionUtils.putOnce(opFactories, op, new IOpNodeFactory<E>() {
			@Override
			public IExprNode<E> create(List<IExprNode<E>> children) {
				Preconditions.checkArgument(children.size() == 1, "Expected one child, got %s", children);
				return factory.create(children.get(0));
			}
		});
		return this;
	}

}
