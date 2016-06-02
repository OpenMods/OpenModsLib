package openmods.calc.types.multi;

import java.lang.reflect.TypeVariable;
import java.util.Map;

import openmods.calc.BinaryOperator;
import openmods.calc.types.multi.TypeDomain.Coercion;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;

public class TypedBinaryOperator extends BinaryOperator<TypedValue> {

	public TypedBinaryOperator(String id, int precedence, openmods.calc.BinaryOperator.Associativity associativity) {
		super(id, precedence, associativity);
	}

	public TypedBinaryOperator(String id, int precendence) {
		super(id, precendence);
	}

	public interface IGenericOperation {
		public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right);
	}

	private static final TypeVariable<?> VAR_T;

	static {
		final TypeVariable<?>[] typeParameters = ICoercedOperation.class.getTypeParameters();
		VAR_T = typeParameters[0];
	}

	private static class CoercedOperationWrapper<T> implements IGenericOperation {
		private final ICoercedOperation<T> op;
		private final Class<? extends T> type;

		public CoercedOperationWrapper(Class<? extends T> type, ICoercedOperation<T> op) {
			this.op = op;
			this.type = type;
		}

		@Override
		public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right) {
			final T leftValue = left.unwrap(type);
			final T rightValue = right.unwrap(type);
			final T result = op.apply(domain, leftValue, rightValue);
			return new TypedValue(left.domain, type, result);
		}

	}

	private final Map<Class<?>, IGenericOperation> coercedOperations = Maps.newHashMap();

	public <T> void registerOperation(Class<? extends T> type, ICoercedOperation<T> op) {
		final IGenericOperation prev = coercedOperations.put(type, new CoercedOperationWrapper<T>(type, op));
		Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s', type: %s", id, type);
	}

	@SuppressWarnings("unchecked")
	public <T> void registerOperation(ICoercedOperation<T> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<T> type = (Class<T>)token.resolveType(VAR_T).getRawType();
		registerOperation(type, op);
	}

	private static final TypeVariable<?> VAR_L;
	private static final TypeVariable<?> VAR_R;

	static {
		final TypeVariable<?>[] typeParameters = IVariantOperation.class.getTypeParameters();
		VAR_L = typeParameters[0];
		VAR_R = typeParameters[1];
	}

	private static class VariantOperationWrapper<L, R> implements IGenericOperation {
		private final IVariantOperation<L, R> op;
		private final Class<? extends L> leftType;
		private final Class<? extends R> rightType;

		public VariantOperationWrapper(Class<? extends L> leftType, Class<? extends R> rightType, IVariantOperation<L, R> op) {
			this.leftType = leftType;
			this.rightType = rightType;
			this.op = op;
		}

		@Override
		public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right) {
			final L leftValue = left.unwrap(leftType);
			final R rightValue = right.unwrap(rightType);
			return op.apply(domain, leftValue, rightValue);
		}

	}

	private final Table<Class<?>, Class<?>, IGenericOperation> variantOperations = HashBasedTable.create();

	public <L, R> void registerOperation(Class<? extends L> left, Class<? extends R> right, IVariantOperation<L, R> op) {
		final IGenericOperation prev = variantOperations.put(left, right, new VariantOperationWrapper<L, R>(left, right, op));
		Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s', types: %s, %s", id, left, right);
	}

	@SuppressWarnings("unchecked")
	public <L, R> void registerOperation(IVariantOperation<L, R> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<L> left = (Class<L>)token.resolveType(VAR_L).getRawType();
		final Class<R> right = (Class<R>)token.resolveType(VAR_R).getRawType();
		registerOperation(left, right, op);
	}

	private IDefaultOperation defaultOperation;

	public void setDefaultOperation(IDefaultOperation defaultOperation) {
		this.defaultOperation = defaultOperation;
	}

	@Override
	protected TypedValue execute(TypedValue left, TypedValue right) {
		Preconditions.checkArgument(left.domain == right.domain, "Values from different domains: %s, %s", left, right);
		final TypeDomain domain = left.domain;

		final Coercion coercionRule = domain.getCoercionRule(left.type, right.type);
		if (coercionRule == Coercion.TO_LEFT) {
			final IGenericOperation op = coercedOperations.get(left.type);
			if (op != null) return op.apply(domain, left, right);
		} else if (coercionRule == Coercion.TO_RIGHT) {
			final IGenericOperation op = coercedOperations.get(right.type);
			if (op != null) return op.apply(domain, left, right);
		}

		final IGenericOperation op = variantOperations.get(left.type, right.type);
		if (op != null) return op.apply(domain, left, right);

		if (defaultOperation != null) {
			final Optional<TypedValue> result = defaultOperation.apply(domain, left, right);
			if (result.isPresent()) return result.get();
		}

		throw new IllegalArgumentException(String.format("Can't apply operation '%s' on values %s,%s", id, left, right));
	}
}
