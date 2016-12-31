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

	public class BinaryOperatorRegistration<O extends BinaryOperator<E>> {
		private final O op;

		private BinaryOperatorRegistration(O op) {
			this.op = op;
		}

		public BinaryOperatorRegistration<O> setDefault() {
			Preconditions.checkState(defaultOperator == null, "Trying to replace default operator: %s -> %s", defaultOperator, op);
			defaultOperator = op;
			return this;
		}

		public O unwrap() {
			return op;
		}
	}

	public <O extends BinaryOperator<E>> BinaryOperatorRegistration<O> registerBinaryOperator(O operator) {
		final IExecutable<E> prev = binaryOperators.put(operator.id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
		return new BinaryOperatorRegistration<O>(operator);
	}

	public <O extends UnaryOperator<E>> O registerUnaryOperator(O operator) {
		final IExecutable<E> prev = unaryOperators.put(operator.id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
		return operator;
	}

	public BinaryOperator<E> registerDefaultOperator(BinaryOperator<E> operator) {
		Preconditions.checkState(defaultOperator == null, "Trying to replace default operator: %s -> %s", defaultOperator, operator);
		defaultOperator = operator;
		return operator;
	}

	public Set<String> allOperators() {
		return Sets.union(binaryOperators.keySet(), unaryOperators.keySet());
	}

	public BinaryOperator<E> getBinaryOperator(String op) {
		return binaryOperators.get(op);
	}

	public UnaryOperator<E> getUnaryOperator(String op) {
		return unaryOperators.get(op);
	}

	public BinaryOperator<E> getDefaultOperator() {
		return defaultOperator;
	}
}
