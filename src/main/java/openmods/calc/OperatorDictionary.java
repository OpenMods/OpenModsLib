package openmods.calc;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import openmods.calc.parsing.BinaryOpNode;
import openmods.calc.parsing.IExprNode;
import openmods.calc.parsing.UnaryOpNode;

public class OperatorDictionary<E> {

	public interface IUnaryExprNodeFactory<E> {
		public IExprNode<E> create(UnaryOperator<E> operator, IExprNode<E> arg);
	}

	public interface IBinaryExprNodeFactory<E> {
		public IExprNode<E> create(BinaryOperator<E> operator, IExprNode<E> left, IExprNode<E> right);
	}

	private final Map<String, BinaryOperator<E>> binaryOperators = Maps.newHashMap();

	private final Map<BinaryOperator<E>, IBinaryExprNodeFactory<E>> binaryExprNodeFactories = Maps.newIdentityHashMap();

	private final Map<String, UnaryOperator<E>> unaryOperators = Maps.newHashMap();

	private final Map<UnaryOperator<E>, IUnaryExprNodeFactory<E>> unaryExprNodeFactories = Maps.newIdentityHashMap();

	private BinaryOperator<E> defaultOperator;

	private final IUnaryExprNodeFactory<E> defaultUnaryExprNodeFactory;

	private final IBinaryExprNodeFactory<E> defaultBinaryExprNodeFactory;

	public static <E> IBinaryExprNodeFactory<E> createDefaultBinaryExprNodeFactory() {
		return new IBinaryExprNodeFactory<E>() {

			@Override
			public IExprNode<E> create(BinaryOperator<E> operator, IExprNode<E> left, IExprNode<E> right) {
				return new BinaryOpNode<E>(operator, left, right);
			}
		};
	}

	public static <E> IUnaryExprNodeFactory<E> createDefaultUnaryExprNodeFactory() {
		return new IUnaryExprNodeFactory<E>() {
			@Override
			public IExprNode<E> create(UnaryOperator<E> operator, IExprNode<E> arg) {
				return new UnaryOpNode<E>(operator, arg);
			}
		};
	}

	public OperatorDictionary(IUnaryExprNodeFactory<E> defaultUnaryExprNodeFactory, IBinaryExprNodeFactory<E> defaultBinaryExprNodeFactory) {
		this.defaultUnaryExprNodeFactory = defaultUnaryExprNodeFactory;
		this.defaultBinaryExprNodeFactory = defaultBinaryExprNodeFactory;
	}

	public OperatorDictionary() {
		this.defaultUnaryExprNodeFactory = createDefaultUnaryExprNodeFactory();
		this.defaultBinaryExprNodeFactory = createDefaultBinaryExprNodeFactory();
	}

	public class BinaryOperatorRegistration {
		private final BinaryOperator<E> op;

		private BinaryOperatorRegistration(BinaryOperator<E> op) {
			this.op = op;
		}

		public BinaryOperatorRegistration setDefault() {
			Preconditions.checkState(defaultOperator == null, "Trying to replace default operator: %s -> %s", defaultOperator, op);
			defaultOperator = op;
			return this;
		}

		public BinaryOperatorRegistration addExprNodeFactory(IBinaryExprNodeFactory<E> factory) {
			final IBinaryExprNodeFactory<E> prev = binaryExprNodeFactories.put(op, factory);
			Preconditions.checkState(prev == null, "Duplicate expr factory");
			return this;
		}
	}

	public BinaryOperatorRegistration registerBinaryOperator(BinaryOperator<E> operator) {
		final IExecutable<E> prev = binaryOperators.put(operator.id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
		return new BinaryOperatorRegistration(operator);
	}

	public class UnaryOperatorRegistration {
		private final UnaryOperator<E> op;

		private UnaryOperatorRegistration(UnaryOperator<E> op) {
			this.op = op;
		}

		public UnaryOperatorRegistration addExprNodeFactory(IUnaryExprNodeFactory<E> factory) {
			final IUnaryExprNodeFactory<E> prev = unaryExprNodeFactories.put(op, factory);
			Preconditions.checkState(prev == null, "Duplicate expr factory");
			return this;
		}
	}

	public UnaryOperatorRegistration registerUnaryOperator(UnaryOperator<E> operator) {
		final IExecutable<E> prev = unaryOperators.put(operator.id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
		return new UnaryOperatorRegistration(operator);
	}

	public Set<String> allOperators() {
		return Sets.union(binaryOperators.keySet(), unaryOperators.keySet());
	}

	public BinaryOperator<E> getBinaryOperator(String op) {
		return binaryOperators.get(op);
	}

	public IExprNode<E> getExprNodeForOperator(BinaryOperator<E> op, IExprNode<E> left, IExprNode<E> right) {
		return Objects.firstNonNull(binaryExprNodeFactories.get(op), defaultBinaryExprNodeFactory).create(op, left, right);
	}

	public UnaryOperator<E> getUnaryOperator(String value) {
		return unaryOperators.get(value);
	}

	public IExprNode<E> getExprNodeForOperator(UnaryOperator<E> op, IExprNode<E> arg) {
		return Objects.firstNonNull(unaryExprNodeFactories.get(op), defaultUnaryExprNodeFactory).create(op, arg);
	}

	public BinaryOperator<E> getDefaultOperator() {
		return defaultOperator;
	}

	// binary first. For RPN purposes second operator must be defined ('-' -> 'neg')
	public Operator<E> getAnyOperator(String value) {
		final Operator<E> op = binaryOperators.get(value);
		if (op != null) return op;

		return unaryOperators.get(value);
	}

}
