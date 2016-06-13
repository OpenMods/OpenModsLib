package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import openmods.calc.BinaryOperator;
import openmods.calc.types.multi.TypeDomain.Coercion;

public class TypedBinaryOperator extends BinaryOperator<TypedValue> {

	public TypedBinaryOperator(String id, int precedence, openmods.calc.BinaryOperator.Associativity associativity) {
		super(id, precedence, associativity);
	}

	public TypedBinaryOperator(String id, int precendence) {
		super(id, precendence);
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> resolveVariable(TypeToken<?> token, TypeVariable<?> var) {
		return (Class<T>)token.resolveType(var).getRawType();
	}

	private interface IGenericOperation {
		public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right);
	}

	public interface ICoercedOperation<T> {
		public TypedValue apply(TypeDomain domain, T left, T right);
	}

	public interface ISimpleCoercedOperation<T> {
		public T apply(T left, T right);
	}

	private final Map<Class<?>, IGenericOperation> coercedOperations = Maps.newHashMap();

	public <T> void registerOperation(final Class<T> type, final ICoercedOperation<T> op) {
		final IGenericOperation prev = coercedOperations.put(type, new IGenericOperation() {
			@Override
			public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right) {
				final T leftValue = left.unwrap(type);
				final T rightValue = right.unwrap(type);
				return op.apply(domain, leftValue, rightValue);
			}

		});

		Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s', type: %s", id, type);
	}

	public <T> void registerOperation(final Class<T> type, final ISimpleCoercedOperation<T> op) {
		registerOperation(type, new ICoercedOperation<T>() {
			@Override
			public TypedValue apply(TypeDomain domain, T left, T right) {
				final T result = op.apply(left, right);
				return domain.create(type, result);
			}
		});
	}

	private static final TypeVariable<?> VAR_COERCED_T;

	static {
		final TypeVariable<?>[] typeParameters = ICoercedOperation.class.getTypeParameters();
		VAR_COERCED_T = typeParameters[0];
	}

	public <T> void registerOperation(ICoercedOperation<T> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<T> type = resolveVariable(token, VAR_COERCED_T);
		registerOperation(type, op);
	}

	private static final TypeVariable<?> VAR_SIMPLE_COERCED_T;

	static {
		final TypeVariable<?>[] typeParameters = ISimpleCoercedOperation.class.getTypeParameters();
		VAR_SIMPLE_COERCED_T = typeParameters[0];
	}

	public <T> void registerOperation(ISimpleCoercedOperation<T> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<T> type = resolveVariable(token, VAR_SIMPLE_COERCED_T);
		registerOperation(type, op);
	}

	public interface IVariantOperation<L, R> {
		public TypedValue apply(TypeDomain domain, L left, R right);
	}

	public interface ISimpleVariantOperation<L, R, O> {
		public O apply(L left, R right);
	}

	private final Table<Class<?>, Class<?>, IGenericOperation> variantOperations = HashBasedTable.create();

	public <L, R> void registerOperation(final Class<? extends L> left, final Class<? extends R> right, final IVariantOperation<L, R> op) {
		final IGenericOperation prev = variantOperations.put(left, right, new IGenericOperation() {
			@Override
			public TypedValue apply(TypeDomain domain, TypedValue leftArg, TypedValue rightArg) {
				final L leftValue = leftArg.unwrap(left);
				final R rightValue = rightArg.unwrap(right);
				return op.apply(domain, leftValue, rightValue);
			}

		});
		Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s', types: %s, %s", id, left, right);
	}

	public <L, R, O> void registerOperation(Class<? extends L> left, Class<? extends R> right, final Class<? super O> output, final ISimpleVariantOperation<L, R, O> op) {
		registerOperation(left, right, new IVariantOperation<L, R>() {
			@Override
			public TypedValue apply(TypeDomain domain, L left, R right) {
				final O result = op.apply(left, right);
				return domain.create(output, result);
			}
		});
	}

	private static final TypeVariable<?> VAR_VARIANT_L;
	private static final TypeVariable<?> VAR_VARIANT_R;

	static {
		final TypeVariable<?>[] typeParameters = IVariantOperation.class.getTypeParameters();
		VAR_VARIANT_L = typeParameters[0];
		VAR_VARIANT_R = typeParameters[1];
	}

	public <L, R> void registerOperation(IVariantOperation<L, R> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<L> left = resolveVariable(token, VAR_VARIANT_L);
		final Class<R> right = resolveVariable(token, VAR_VARIANT_R);
		registerOperation(left, right, op);
	}

	private static final TypeVariable<?> VAR_SIMPLE_VARIANT_L;
	private static final TypeVariable<?> VAR_SIMPLE_VARIANT_R;
	private static final TypeVariable<?> VAR_SIMPLE_VARIANT_O;

	static {
		final TypeVariable<?>[] typeParameters = ISimpleVariantOperation.class.getTypeParameters();
		VAR_SIMPLE_VARIANT_L = typeParameters[0];
		VAR_SIMPLE_VARIANT_R = typeParameters[1];
		VAR_SIMPLE_VARIANT_O = typeParameters[2];
	}

	public <L, R, O> void registerOperation(ISimpleVariantOperation<L, R, O> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<L> left = resolveVariable(token, VAR_SIMPLE_VARIANT_L);
		final Class<R> right = resolveVariable(token, VAR_SIMPLE_VARIANT_R);
		final Class<O> output = resolveVariable(token, VAR_SIMPLE_VARIANT_O);
		registerOperation(left, right, output, op);
	}

	public interface IDefaultOperation {
		public Optional<TypedValue> apply(TypeDomain domain, TypedValue left, TypedValue right);
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
