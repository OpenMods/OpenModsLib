package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import openmods.calc.BinaryOperator;

public abstract class TypedBinaryBooleanOperator extends BinaryOperator<TypedValue> {

	public TypedBinaryBooleanOperator(String id, int precedence, BinaryOperator.Associativity associativity) {
		super(id, precedence, associativity);
	}

	public TypedBinaryBooleanOperator(String id, int precendence) {
		super(id, precendence);
	}

	@Override
	public TypedValue execute(TypedValue left, TypedValue right) {
		Preconditions.checkArgument(left.domain == right.domain, "Values from different domains: %s, %s", left, right);
		final Optional<Boolean> isTruthy = left.isTruthy();
		Preconditions.checkState(isTruthy.isPresent(), "Can't determine truth value for %s", left);
		return execute(isTruthy.get(), left, right);
	}

	protected abstract TypedValue execute(boolean isTruthy, TypedValue left, TypedValue right);

}
