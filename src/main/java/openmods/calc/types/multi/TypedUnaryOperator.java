package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import openmods.calc.UnaryOperator;
import openmods.reflection.TypeVariableHolder;

public class TypedUnaryOperator extends UnaryOperator<TypedValue> {

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

	public TypedUnaryOperator(String id) {
		super(id);
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> resolveVariable(TypeToken<?> token, TypeVariable<?> var) {
		return (Class<T>)token.resolveType(var).getRawType();
	}

	private interface IGenericOperation {
		public TypedValue apply(TypeDomain domain, TypedValue left);
	}

	private Map<Class<?>, IGenericOperation> operations = Maps.newHashMap();

	public interface IOperation<A> {
		public TypedValue apply(TypeDomain domain, A value);
	}

	public interface ISimpleOperation<A, R> {
		public R apply(A value);
	}

	public <A> TypedUnaryOperator registerOperation(final Class<? extends A> argCls, final IOperation<A> op) {
		final IGenericOperation prev = operations.put(argCls, new IGenericOperation() {
			@Override
			public TypedValue apply(TypeDomain domain, TypedValue left) {
				final A arg = left.unwrap(argCls);
				return op.apply(domain, arg);
			}
		});
		Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s' for type %s", id, argCls);
		return this;
	}

	public <A, R> TypedUnaryOperator registerOperation(Class<? extends A> argCls, final Class<? super R> resultCls, final ISimpleOperation<A, R> op) {
		return registerOperation(argCls, new IOperation<A>() {
			@Override
			public TypedValue apply(TypeDomain domain, A value) {
				final R result = op.apply(value);
				return domain.create(resultCls, result);
			}
		});
	}

	public <A> TypedUnaryOperator registerOperation(IOperation<A> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<A> type = resolveVariable(token, TypeVariableHolders.Operation.A);
		return registerOperation(type, op);
	}

	public <A, R> TypedUnaryOperator registerOperation(ISimpleOperation<A, R> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<A> argType = resolveVariable(token, TypeVariableHolders.SimpleOperation.A);
		final Class<R> resultType = resolveVariable(token, TypeVariableHolders.SimpleOperation.R);
		return registerOperation(argType, resultType, op);
	}

	public interface IDefaultOperation {
		public Optional<TypedValue> apply(TypeDomain domain, TypedValue value);
	}

	private IDefaultOperation defaultOperation;

	public TypedUnaryOperator setDefaultOperation(IDefaultOperation defaultOperation) {
		this.defaultOperation = defaultOperation;
		return this;
	}

	@Override
	protected TypedValue execute(TypedValue value) {
		final IGenericOperation op = operations.get(value.type);
		if (op != null) return op.apply(value.domain, value);

		if (defaultOperation != null) {
			final Optional<TypedValue> result = defaultOperation.apply(value.domain, value);
			if (result.isPresent()) return result.get();
		}

		throw new IllegalArgumentException(String.format("Can't apply operation '%s' on value %s", id, value));
	}
}
