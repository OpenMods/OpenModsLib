package openmods.calc;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class OperatorDictionary<E> {

	private final Map<String, BinaryOperator<E>> binaryOperators = Maps.newHashMap();

	private final Map<String, UnaryOperator<E>> unaryOperators = Maps.newHashMap();

	public void registerBinaryOperator(String id, BinaryOperator<E> operator) {
		final IExecutable<E> prev = binaryOperators.put(id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
	}

	public void registerUnaryOperator(String id, UnaryOperator<E> operator) {
		final IExecutable<E> prev = unaryOperators.put(id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
	}

	public void registerMixedOperator(String id, BinaryOperator<E> binaryOperator, UnaryOperator<E> unaryOperator) {
		registerBinaryOperator(id, binaryOperator);
		registerUnaryOperator(id, unaryOperator);
	}

	public Set<String> allOperators() {
		return Sets.union(binaryOperators.keySet(), unaryOperators.keySet());
	}

	public Operator<E> getBinaryOperator(String op) {
		return binaryOperators.get(op);
	}

	public Operator<E> getUnaryOperator(String value) {
		return unaryOperators.get(value);
	}

	// binary first. For RPN purposes second operator must be defined ('-' -> 'neg')
	public Operator<E> getAnyOperator(String value) {
		final Operator<E> op = binaryOperators.get(value);
		if (op != null) return op;

		return unaryOperators.get(value);
	}

}
