package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import openmods.calc.BinaryOperator;
import openmods.calc.types.multi.TypeDomain.Coercion;
import openmods.reflection.TypeVariableHolder;

public class TypedBinaryOperator extends BinaryOperator<TypedValue> {

	public interface ICoercedOperation<T> {
		public TypedValue apply(TypeDomain domain, T left, T right);
	}

	public interface ISimpleCoercedOperation<T, O> {
		public O apply(T left, T right);
	}

	public interface IVariantOperation<L, R> {
		public TypedValue apply(TypeDomain domain, L left, R right);
	}

	public interface ISimpleVariantOperation<L, R, O> {
		public O apply(L left, R right);
	}

	public interface IDefaultOperation {
		public Optional<TypedValue> apply(TypeDomain domain, TypedValue left, TypedValue right);
	}

	public static class TypeVariableHolders {
		@TypeVariableHolder(ICoercedOperation.class)
		public static class CoercedOperation {
			public static TypeVariable<?> T;
		}

		@TypeVariableHolder(ISimpleCoercedOperation.class)
		public static class SimpleCoercedOperation {
			public static TypeVariable<?> T;
			public static TypeVariable<?> O;
		}

		@TypeVariableHolder(IVariantOperation.class)
		public static class VariantOperation {
			public static TypeVariable<?> L;
			public static TypeVariable<?> R;
		}

		@TypeVariableHolder(ISimpleVariantOperation.class)
		public static class SimpleVariantOperation {
			public static TypeVariable<?> L;
			public static TypeVariable<?> R;
			public static TypeVariable<?> O;
		}
	}

	private interface IGenericOperation {
		public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right);

		public void validate(TypeDomain domain);
	}

	public static class Builder {
		private final String id;

		private final int precedence;

		private final BinaryOperator.Associativity associativity;

		private final Map<Class<?>, IGenericOperation> coercedOperations = Maps.newHashMap();

		private final Table<Class<?>, Class<?>, IGenericOperation> variantOperations = HashBasedTable.create();

		private IDefaultOperation defaultOperation;

		public Builder(String id, int precedence, BinaryOperator.Associativity associativity) {
			this.id = id;
			this.precedence = precedence;
			this.associativity = associativity;
		}

		public Builder(String id, int precedence) {
			this(id, precedence, BinaryOperator.DEFAULT_ASSOCIATIVITY);
		}

		@SuppressWarnings("unchecked")
		private static <T> Class<T> resolveVariable(TypeToken<?> token, TypeVariable<?> var) {
			return (Class<T>)token.resolveType(var).getRawType();
		}

		private Builder registerCoercedOperation(Class<?> type, IGenericOperation op) {
			final IGenericOperation prev = coercedOperations.put(type, op);
			Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s', type: %s", id, type);
			return this;
		}

		public <T> Builder registerOperation(Class<T> type, ICoercedOperation<? super T> op) {
			return registerCoercedOperation(type, createOperationWrapper(type, op));
		}

		private static <T> IGenericOperation createOperationWrapper(final Class<T> type, final ICoercedOperation<? super T> op) {
			return new IGenericOperation() {
				@Override
				public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right) {
					final T leftValue = left.unwrap(type);
					final T rightValue = right.unwrap(type);
					return op.apply(domain, leftValue, rightValue);
				}

				@Override
				public void validate(TypeDomain domain) {
					Preconditions.checkState(domain.isKnownType(type), "Type %s not in domain", type);
				}
			};
		}

		public <T, O> Builder registerOperation(Class<T> type, Class<O> output, ISimpleCoercedOperation<? super T, ? extends O> op) {
			return registerCoercedOperation(type, createOperationWrapper(type, output, op));
		}

		private static <T, O> IGenericOperation createOperationWrapper(final Class<T> type, final Class<O> output, final ISimpleCoercedOperation<? super T, ? extends O> op) {
			return new IGenericOperation() {
				@Override
				public TypedValue apply(TypeDomain domain, TypedValue left, TypedValue right) {
					final T leftValue = left.unwrap(type);
					final T rightValue = right.unwrap(type);
					final O result = op.apply(leftValue, rightValue);
					return new TypedValue(domain, output, result);
				}

				@Override
				public void validate(TypeDomain domain) {
					Preconditions.checkState(domain.isKnownType(type), "Parameter type %s not in domain", type);
					Preconditions.checkState(domain.isKnownType(output), "Output type %s not in domain", output);
				}
			};
		}

		public <T> Builder registerOperation(ICoercedOperation<T> op) {
			final TypeToken<?> token = TypeToken.of(op.getClass());
			final Class<T> type = resolveVariable(token, TypeVariableHolders.CoercedOperation.T);
			return registerOperation(type, op);
		}

		public <T, O> Builder registerOperation(ISimpleCoercedOperation<T, O> op) {
			final TypeToken<?> token = TypeToken.of(op.getClass());
			final Class<T> type = resolveVariable(token, TypeVariableHolders.SimpleCoercedOperation.T);
			final Class<O> output = resolveVariable(token, TypeVariableHolders.SimpleCoercedOperation.O);
			return registerOperation(type, output, op);
		}

		private Builder registerVariantOperation(Class<?> left, Class<?> right, IGenericOperation op) {
			final IGenericOperation prev = variantOperations.put(left, right, op);
			Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s', types: %s, %s", id, left, right);
			return this;
		}

		public <L, R> Builder registerOperation(Class<? extends L> left, Class<? extends R> right, IVariantOperation<? super L, ? super R> op) {
			return registerVariantOperation(left, right, createOperationWrapper(left, right, op));
		}

		private static <L, R> IGenericOperation createOperationWrapper(final Class<? extends L> left, final Class<? extends R> right, final IVariantOperation<? super L, ? super R> op) {
			return new IGenericOperation() {
				@Override
				public TypedValue apply(TypeDomain domain, TypedValue leftArg, TypedValue rightArg) {
					final L leftValue = leftArg.unwrap(left);
					final R rightValue = rightArg.unwrap(right);
					return op.apply(domain, leftValue, rightValue);
				}

				@Override
				public void validate(TypeDomain domain) {
					Preconditions.checkState(domain.isKnownType(left), "Left parameter type %s not in domain", left);
					Preconditions.checkState(domain.isKnownType(right), "Right parameter type %s not in domain", right);
				}

			};
		}

		public <L, R, O> Builder registerOperation(Class<? extends L> left, Class<? extends R> right, Class<? super O> output, ISimpleVariantOperation<? super L, ? super R, ? extends O> op) {
			return registerVariantOperation(left, right, createOperationWrapper(left, right, output, op));
		}

		private static <O, R, L> IGenericOperation createOperationWrapper(final Class<? extends L> left, final Class<? extends R> right, final Class<? super O> output, final ISimpleVariantOperation<? super L, ? super R, ? extends O> op) {
			return new IGenericOperation() {
				@Override
				public TypedValue apply(TypeDomain domain, TypedValue leftArg, TypedValue rightArg) {
					final L leftValue = leftArg.unwrap(left);
					final R rightValue = rightArg.unwrap(right);
					final O result = op.apply(leftValue, rightValue);
					return new TypedValue(domain, output, result);
				}

				@Override
				public void validate(TypeDomain domain) {
					Preconditions.checkState(domain.isKnownType(left), "Left parameter type %s not in domain", left);
					Preconditions.checkState(domain.isKnownType(right), "Right parameter type %s not in domain", right);
					Preconditions.checkState(domain.isKnownType(output), "Output type %s not in domain", output);
				}
			};
		}

		public <L, R> Builder registerOperation(IVariantOperation<L, R> op) {
			final TypeToken<?> token = TypeToken.of(op.getClass());
			final Class<L> left = resolveVariable(token, TypeVariableHolders.VariantOperation.L);
			final Class<R> right = resolveVariable(token, TypeVariableHolders.VariantOperation.R);
			return registerOperation(left, right, op);
		}

		public <L, R, O> Builder registerOperation(ISimpleVariantOperation<L, R, O> op) {
			final TypeToken<?> token = TypeToken.of(op.getClass());
			final Class<L> left = resolveVariable(token, TypeVariableHolders.SimpleVariantOperation.L);
			final Class<R> right = resolveVariable(token, TypeVariableHolders.SimpleVariantOperation.R);
			final Class<O> output = resolveVariable(token, TypeVariableHolders.SimpleVariantOperation.O);
			return registerOperation(left, right, output, op);
		}

		public Builder setDefaultOperation(IDefaultOperation defaultOperation) {
			this.defaultOperation = defaultOperation;
			return this;
		}

		public TypedBinaryOperator build(TypeDomain domain) {
			for (IGenericOperation op : coercedOperations.values())
				op.validate(domain);

			for (IGenericOperation op : variantOperations.values())
				op.validate(domain);

			return new TypedBinaryOperator(id, precedence, associativity, domain, coercedOperations, variantOperations, defaultOperation);
		}
	}

	private final Map<Class<?>, IGenericOperation> coercedOperations;

	private final Table<Class<?>, Class<?>, IGenericOperation> variantOperations;

	private final IDefaultOperation defaultOperation;

	private final TypeDomain domain;

	private TypedBinaryOperator(String id, int precedence, Associativity associativity,
			TypeDomain domain,
			Map<Class<?>, IGenericOperation> coercedOperations,
			Table<Class<?>, Class<?>, IGenericOperation> variantOperations,
			IDefaultOperation defaultOperation) {
		super(id, precedence, associativity);
		this.coercedOperations = ImmutableMap.copyOf(coercedOperations);
		this.variantOperations = ImmutableTable.copyOf(variantOperations);
		this.defaultOperation = defaultOperation;
		this.domain = domain;
	}

	@Override
	public TypedValue execute(TypedValue left, TypedValue right) {
		Preconditions.checkArgument(left.domain == this.domain, "Left argument belongs to different domain: %s", left);
		Preconditions.checkArgument(right.domain == this.domain, "Right argument belongs different domain: %s", right);

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
