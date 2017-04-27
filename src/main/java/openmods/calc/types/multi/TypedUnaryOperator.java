package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import openmods.calc.executable.UnaryOperator;
import openmods.reflection.TypeVariableHolder;

public class TypedUnaryOperator extends UnaryOperator.Direct<TypedValue> {

	public static class TypeVariableHolders {
		@TypeVariableHolder(IOperation.class)
		public static class Operation {
			public static TypeVariable<?> A;
		}

		@TypeVariableHolder(ISimpleOperation.class)
		public static class SimpleOperation {
			public static TypeVariable<?> A;
			public static TypeVariable<?> R;
		}
	}

	public interface IOperation<A> {
		public TypedValue apply(TypeDomain domain, A value);
	}

	public interface ISimpleOperation<A, R> {
		public R apply(A value);
	}

	private interface IGenericOperation {
		public TypedValue apply(TypeDomain domain, TypedValue left);

		public void validate(TypeDomain domain);
	}

	public static class Builder {
		private final String id;

		private final Map<Class<?>, IGenericOperation> operations = Maps.newHashMap();

		private IDefaultOperation defaultOperation;

		public Builder(String id) {
			this.id = id;
		}

		@SuppressWarnings("unchecked")
		private static <T> Class<T> resolveVariable(TypeToken<?> token, TypeVariable<?> var) {
			return (Class<T>)token.resolveType(var).getRawType();
		}

		private Builder registerOperation(Class<?> argCls, final IGenericOperation op) {
			final IGenericOperation prev = operations.put(argCls, op);
			Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s' for type %s", id, argCls);
			return this;
		}

		public <A> Builder registerOperation(Class<? extends A> argCls, IOperation<? super A> op) {
			return registerOperation(argCls, createOperationWrapper(argCls, op));
		}

		private static <A> IGenericOperation createOperationWrapper(final Class<? extends A> argCls, final IOperation<? super A> op) {
			return new IGenericOperation() {
				@Override
				public TypedValue apply(TypeDomain domain, TypedValue argValue) {
					final A arg = argValue.unwrap(argCls);
					return op.apply(domain, arg);
				}

				@Override
				public void validate(TypeDomain domain) {
					Preconditions.checkState(domain.isKnownType(argCls), "Parameter type %s not in domain", argCls);
				}
			};
		}

		public <A, R> Builder registerOperation(Class<? extends A> argCls, Class<? super R> resultCls, ISimpleOperation<? super A, ? extends R> op) {
			return registerOperation(argCls, createOperationWrapper(argCls, resultCls, op));
		}

		private static <A, R> IGenericOperation createOperationWrapper(final Class<? extends A> argCls, final Class<? super R> resultCls, final ISimpleOperation<? super A, ? extends R> op) {
			return new IGenericOperation() {
				@Override
				public TypedValue apply(TypeDomain domain, TypedValue argValue) {
					final A value = argValue.unwrap(argCls);
					final R result = op.apply(value);
					return domain.create(resultCls, result);
				}

				@Override
				public void validate(TypeDomain domain) {
					Preconditions.checkState(domain.isKnownType(argCls), "Parameter type %s not in domain", argCls);
					Preconditions.checkState(domain.isKnownType(resultCls), "Return type %s not in domain", resultCls);
				}
			};
		}

		public <A> Builder registerOperation(IOperation<A> op) {
			final TypeToken<?> token = TypeToken.of(op.getClass());
			final Class<A> type = resolveVariable(token, TypeVariableHolders.Operation.A);
			return registerOperation(type, op);
		}

		public <A, R> Builder registerOperation(ISimpleOperation<A, R> op) {
			final TypeToken<?> token = TypeToken.of(op.getClass());
			final Class<A> argType = resolveVariable(token, TypeVariableHolders.SimpleOperation.A);
			final Class<R> resultType = resolveVariable(token, TypeVariableHolders.SimpleOperation.R);
			return registerOperation(argType, resultType, op);
		}

		public Builder setDefaultOperation(IDefaultOperation defaultOperation) {
			this.defaultOperation = defaultOperation;
			return this;
		}

		public TypedUnaryOperator build(TypeDomain domain) {
			for (IGenericOperation op : operations.values())
				op.validate(domain);

			return new TypedUnaryOperator(id, operations, defaultOperation, domain);
		}
	}

	public TypedUnaryOperator(String id, Map<Class<?>, IGenericOperation> operations, IDefaultOperation defaultOperation, TypeDomain domain) {
		super(id);
		this.operations = ImmutableMap.copyOf(operations);
		this.defaultOperation = defaultOperation;
		this.domain = domain;
	}

	private final Map<Class<?>, IGenericOperation> operations;

	private final IDefaultOperation defaultOperation;

	private final TypeDomain domain;

	public interface IDefaultOperation {
		public Optional<TypedValue> apply(TypeDomain domain, TypedValue value);
	}

	@Override
	public TypedValue execute(TypedValue value) {
		Preconditions.checkState(value.domain == this.domain, "Value belongs to different domain: %s", value);
		final IGenericOperation op = operations.get(value.type);
		if (op != null) return op.apply(value.domain, value);

		if (defaultOperation != null) {
			final Optional<TypedValue> result = defaultOperation.apply(value.domain, value);
			if (result.isPresent()) return result.get();
		}

		throw new IllegalArgumentException(String.format("Can't apply operation '%s' on value %s", id, value));
	}
}
