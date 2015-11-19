package openmods.calc;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class OperatorDictionary<E> {

	private final Map<String, IOperator<E>> operators = Maps.newHashMap();

	private final Map<String, IOperator<E>> unaryVariants = Maps.newHashMap();

	public void registerOperator(String id, IOperator<E> operator) {
		final IExecutable<E> prev = operators.put(id, operator);
		Preconditions.checkState(prev == null, "Duplicate operator '%s': %s -> %s", prev, operator);
	}

	public void registerOperator(String binaryId, IOperator<E> binaryOp, String unaryId, IOperator<E> unaryOp) {
		registerOperator(binaryId, binaryOp);
		registerOperator(unaryId, unaryOp);

		unaryVariants.put(binaryId, unaryOp);
	}

	public void registerOperator(String binaryId, IOperator<E> binaryOp, IOperator<E> unaryId) {
		registerOperator(binaryId, binaryOp);
		unaryVariants.put(binaryId, unaryId);
	}

	public Set<String> allOperators() {
		return operators.keySet();
	}

	public IOperator<E> get(String op) {
		return operators.get(op);
	}

	public IOperator<E> getUnaryVariant(String value) {
		return unaryVariants.get(value);
	}

}
