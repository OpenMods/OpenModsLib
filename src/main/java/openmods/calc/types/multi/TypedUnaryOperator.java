package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import openmods.calc.UnaryOperator;

public class TypedUnaryOperator extends UnaryOperator<TypedValue> {

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

	public <A> void registerOperation(final Class<? extends A> argCls, final IOperation<A> op) {
		final IGenericOperation prev = operations.put(argCls, new IGenericOperation() {
			@Override
			public TypedValue apply(TypeDomain domain, TypedValue left) {
				final A arg = left.unwrap(argCls);
				return op.apply(domain, arg);
			}
		});
		Preconditions.checkState(prev == null, "Duplicate operation registration on operator '%s' for type %s", id, argCls);
	}

	public <A, R> void registerOperation(Class<? extends A> argCls, final Class<? super R> resultCls, final ISimpleOperation<A, R> op) {
		registerOperation(argCls, new IOperation<A>() {
			@Override
			public TypedValue apply(TypeDomain domain, A value) {
				final R result = op.apply(value);
				return domain.create(resultCls, result);
			}
		});
	}

	private static final TypeVariable<?> VAR_A;

	static {
		final TypeVariable<?>[] typeParameters = IOperation.class.getTypeParameters();
		VAR_A = typeParameters[0];
	}

	public <A> void registerOperation(IOperation<A> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<A> type = resolveVariable(token, VAR_A);
		registerOperation(type, op);
	}

	private static final TypeVariable<?> VAR_SIMPLE_A;
	private static final TypeVariable<?> VAR_SIMPLE_R;

	static {
		final TypeVariable<?>[] typeParameters = ISimpleOperation.class.getTypeParameters();
		VAR_SIMPLE_A = typeParameters[0];
		VAR_SIMPLE_R = typeParameters[1];
	}

	public <A, R> void registerOperation(ISimpleOperation<A, R> op) {
		final TypeToken<?> token = TypeToken.of(op.getClass());
		final Class<A> argType = resolveVariable(token, VAR_SIMPLE_A);
		final Class<R> resultType = resolveVariable(token, VAR_SIMPLE_R);
		registerOperation(argType, resultType, op);
	}

	public interface IDefaultOperation {
		public Optional<TypedValue> apply(TypeDomain domain, TypedValue value);
	}

	private IDefaultOperation defaultOperation;

	public void setDefaultOperation(IDefaultOperation defaultOperation) {
		this.defaultOperation = defaultOperation;
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
