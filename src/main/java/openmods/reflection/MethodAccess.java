package openmods.reflection;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.List;

public class MethodAccess {

	public interface FunctionBase<R> {}

	public interface FunctionVar<R> extends FunctionBase<R> {
		public R call(Object target, Object... args);
	}

	private static class FunctionWrap<R> implements FunctionVar<R> {
		private final Method method;

		public FunctionWrap(Class<? extends R> returnCls, Method method) {
			this.method = method;
			method.setAccessible(true);
			Preconditions.checkArgument(returnCls.isAssignableFrom(method.getReturnType()), "Method '%s' has invalid return type", method);
		}

		@Override
		@SuppressWarnings("unchecked")
		public R call(Object target, Object... args) {
			try {
				return (R)method.invoke(target, args);
			} catch (Throwable t) {
				throw Throwables.propagate(t);
			}
		}
	}

	// R()

	public interface Function0<R> extends FunctionBase<R> {
		public R call(Object target);
	}

	private static class Function0Impl<R> extends FunctionWrap<R> implements Function0<R> {
		public Function0Impl(Class<? extends R> returnCls, Method method) {
			super(returnCls, method);
		}

		@Override
		public R call(Object target) {
			return super.call(target);
		}
	}

	public static <R> Function0<R> create(Class<? extends R> returnCls, Class<?> target, String... names) {
		return new Function0Impl<R>(returnCls, ReflectionHelper.getMethod(target, names));
	}

	// R(P1)

	public interface Function1<R, P1> extends FunctionBase<R> {
		public R call(Object target, P1 p1);
	}

	private static class Function1Impl<R, P1> extends FunctionWrap<R> implements Function1<R, P1> {
		public Function1Impl(Class<? extends R> returnCls, Method method) {
			super(returnCls, method);
		}

		@Override
		public R call(Object target, P1 p1) {
			return super.call(target, p1);
		}
	}

	public static <R, P1> Function1<R, P1> create(Class<? extends R> returnCls, Class<?> target, Class<? extends P1> p1, String... names) {
		return new Function1Impl<R, P1>(returnCls, ReflectionHelper.getMethod(target, names, p1));
	}

	// R(P1, P2)

	public interface Function2<R, P1, P2> extends FunctionBase<R> {
		public R call(Object target, P1 p1, P2 p2);
	}

	private static class Function2Impl<R, P1, P2> extends FunctionWrap<R> implements Function2<R, P1, P2> {
		public Function2Impl(Class<? extends R> returnCls, Method method) {
			super(returnCls, method);
		}

		@Override
		public R call(Object target, P1 p1, P2 p2) {
			return super.call(target, p1, p2);
		}
	}

	public static <R, P1, P2> Function2<R, P1, P2> create(Class<? extends R> returnCls, Class<?> target, Class<? extends P1> p1, Class<? extends P2> p2, String... names) {
		return new Function2Impl<R, P1, P2>(returnCls, ReflectionHelper.getMethod(target, names, p1, p2));
	}

	// R(P1, P2, P3)

	public interface Function3<R, P1, P2, P3> extends FunctionBase<R> {
		public R call(Object target, P1 p1, P2 p2, P3 p3);
	}

	private static class Function3Impl<R, P1, P2, P3> extends FunctionWrap<R> implements Function3<R, P1, P2, P3> {
		public Function3Impl(Class<? extends R> returnCls, Method method) {
			super(returnCls, method);
		}

		@Override
		public R call(Object target, P1 p1, P2 p2, P3 p3) {
			return super.call(target, p1, p2, p3);
		}
	}

	public static <R, P1, P2, P3> Function3<R, P1, P2, P3> create(Class<? extends R> returnCls, Class<?> target, Class<? extends P1> p1, Class<? extends P2> p2, Class<? extends P3> p3, String... names) {
		return new Function3Impl<R, P1, P2, P3>(returnCls, ReflectionHelper.getMethod(target, names, p1, p2, p3));
	}

	// R(P1, P2, P3, P4)

	public interface Function4<R, P1, P2, P3, P4> extends FunctionBase<R> {
		public R call(Object target, P1 p1, P2 p2, P3 p3, P4 p4);
	}

	private static class Function4Impl<R, P1, P2, P3, P4> extends FunctionWrap<R> implements Function4<R, P1, P2, P3, P4> {
		public Function4Impl(Class<? extends R> returnCls, Method method) {
			super(returnCls, method);
		}

		@Override
		public R call(Object target, P1 p1, P2 p2, P3 p3, P4 p4) {
			return super.call(target, p1, p2, p3, p4);
		}
	}

	public static <R, P1, P2, P3, P4> Function4<R, P1, P2, P3, P4> create(Class<? extends R> returnCls, Class<?> target, Class<? extends P1> p1, Class<? extends P2> p2, Class<? extends P3> p3, Class<? extends P4> p4, String... names) {
		return new Function4Impl<R, P1, P2, P3, P4>(returnCls, ReflectionHelper.getMethod(target, names, p1, p2, p3, p4));
	}

	// R(P1, P2, P3, P4, P5)

	public interface Function5<R, P1, P2, P3, P4, P5> extends FunctionBase<R> {
		public R call(Object target, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
	}

	private static class Function5Impl<R, P1, P2, P3, P4, P5> extends FunctionWrap<R> implements Function5<R, P1, P2, P3, P4, P5> {
		public Function5Impl(Class<? extends R> returnCls, Method method) {
			super(returnCls, method);
		}

		@Override
		public R call(Object target, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
			return super.call(target, p1, p2, p3, p4, p5);
		}
	}

	public static <R, P1, P2, P3, P4, P5> Function5<R, P1, P2, P3, P4, P5> create(Class<? extends R> returnCls, Class<?> target, Class<? extends P1> p1, Class<? extends P2> p2, Class<? extends P3> p3, Class<? extends P4> p4, Class<? extends P5> p5, String... names) {
		return new Function5Impl<R, P1, P2, P3, P4, P5>(returnCls, ReflectionHelper.getMethod(target, names, p1, p2, p3, p4, p5));
	}

	// helpers

	public static class TypeVariableHolders {
		@TypeVariableHolder(FunctionBase.class)
		public static class FunctionBaseHolder {
			public static TypeVariable<?> R;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] {};
			}
		}

		@TypeVariableHolder(FunctionVar.class)
		public static class FunctionVarHolder {
			public static TypeVariable<?> R;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] {};
			}
		}

		@TypeVariableHolder(Function0.class)
		public static class Function0Holder {
			public static TypeVariable<?> R;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] {};
			}
		}

		@TypeVariableHolder(Function1.class)
		public static class Function1Holder {
			public static TypeVariable<?> R;
			public static TypeVariable<?> P1;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] { P1 };
			}
		}

		@TypeVariableHolder(Function2.class)
		public static class Function2Holder {
			public static TypeVariable<?> R;
			public static TypeVariable<?> P1;
			public static TypeVariable<?> P2;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] { P1, P2 };
			}
		}

		@TypeVariableHolder(Function3.class)
		public static class Function3Holder {
			public static TypeVariable<?> R;
			public static TypeVariable<?> P1;
			public static TypeVariable<?> P2;
			public static TypeVariable<?> P3;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] { P1, P2, P3 };
			}
		}

		@TypeVariableHolder(Function4.class)
		public static class Function4Holder {
			public static TypeVariable<?> R;
			public static TypeVariable<?> P1;
			public static TypeVariable<?> P2;
			public static TypeVariable<?> P3;
			public static TypeVariable<?> P4;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] { P1, P2, P3, P4 };
			}
		}

		@TypeVariableHolder(Function5.class)
		public static class Function5Holder {
			public static TypeVariable<?> R;
			public static TypeVariable<?> P1;
			public static TypeVariable<?> P2;
			public static TypeVariable<?> P3;
			public static TypeVariable<?> P4;
			public static TypeVariable<?> P5;

			public static TypeVariable<?>[] args() {
				return new TypeVariable<?>[] { P1, P2, P3, P4, P5 };
			}
		}
	}

	private static class ArgResolver {
		private final Class<?> intf;
		private final TypeVariable<?>[] args;

		public ArgResolver(Class<?> intf, TypeVariable<?>... args) {
			this.intf = intf;
			this.args = args;
		}

		public boolean canResolve(Class<?> cls) {
			return intf.isAssignableFrom(cls);
		}

		public Class<?>[] resolve(Class<?> cls) {
			final TypeToken<?> type = TypeToken.of(cls);
			final Class<?>[] result = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++)
				result[i] = type.resolveType(args[i]).getRawType();
			return result;
		}
	}

	private static List<ArgResolver> resolvers;

	@SuppressWarnings("unchecked")
	public static <T> Class<T> resolveReturnType(Class<? extends FunctionBase<T>> cls) {
		final TypeToken<?> type = TypeToken.of(cls);
		return (Class<T>)type.resolveType(TypeVariableHolders.FunctionBaseHolder.R).getRawType();
	}

	public static Class<?>[] resolveParameterTypes(Class<? extends FunctionBase<?>> cls) {
		if (resolvers == null) {
			final ImmutableList.Builder<ArgResolver> builder = ImmutableList.builder();
			builder.add(new ArgResolver(Function0.class, TypeVariableHolders.Function0Holder.args()));
			builder.add(new ArgResolver(Function1.class, TypeVariableHolders.Function1Holder.args()));
			builder.add(new ArgResolver(Function2.class, TypeVariableHolders.Function2Holder.args()));
			builder.add(new ArgResolver(Function3.class, TypeVariableHolders.Function3Holder.args()));
			builder.add(new ArgResolver(Function4.class, TypeVariableHolders.Function4Holder.args()));
			builder.add(new ArgResolver(Function5.class, TypeVariableHolders.Function5Holder.args()));
			resolvers = builder.build();
		}

		final List<ArgResolver> applicableResolvers = Lists.newArrayListWithCapacity(resolvers.size());

		for (ArgResolver r : resolvers)
			if (r.canResolve(cls)) applicableResolvers.add(r);

		Preconditions.checkArgument(!applicableResolvers.isEmpty(), "Invalid type: %s", cls);
		Preconditions.checkArgument(applicableResolvers.size() == 1, "Ambiguous type: %s, bases: ", cls,
				Lists.transform(applicableResolvers, new Function<ArgResolver, String>() {
					@Override
					public String apply(ArgResolver input) {
						return input.intf.getName();
					}
				}));

		return applicableResolvers.get(0).resolve(cls);
	}
}
