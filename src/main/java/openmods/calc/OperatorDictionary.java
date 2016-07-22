package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;

public class OperatorDictionary<E> {

	private final Map<String, BinaryOperator<E>> binaryOperators = Maps.newHashMap();

	private final Map<String, UnaryOperator<E>> unaryOperators = Maps.newHashMap();

	private BinaryOperator<E> defaultOperator;

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

		public BinaryOperator<E> unwrap() {
			return op;
		}
	}

	public BinaryOperatorRegistration registerBinaryOperator(BinaryOperator<E> operator) {
		final IExecutable<E> prev = binaryOperators.put(operator.id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
		return new BinaryOperatorRegistration(operator);
	}

	public void registerUnaryOperator(UnaryOperator<E> operator) {
		final IExecutable<E> prev = unaryOperators.put(operator.id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
	}

	public Set<String> allOperators() {
		return Sets.union(binaryOperators.keySet(), unaryOperators.keySet());
	}

	public BinaryOperator<E> getBinaryOperator(String op) {
		return binaryOperators.get(op);
	}

	public UnaryOperator<E> getUnaryOperator(String value) {
		return unaryOperators.get(value);
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
