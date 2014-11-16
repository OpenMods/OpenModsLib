package openmods.reflection;

import java.lang.reflect.Method;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class MethodAccess {

	public interface FunctionVar<R> {
		public R call(Object target, Object... args);
	}

	private static class FunctionWrap<R> implements FunctionVar<R> {
		private final Method method;

		public FunctionWrap(Class<? extends R> returnCls, Method method) {
			this.method = method;
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

	public interface Function0<R> {
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

	public interface Function1<R, P1> {
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

	public interface Function2<R, P1, P2> {
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

	public interface Function3<R, P1, P2, P3> {
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

	public interface Function4<R, P1, P2, P3, P4> {
		public R call(Object target, P1 p1, P2 p2, P3 p3, P4 p4);
	}

	private static class Function4Impl<R, P1, P2, P3, P4> extends FunctionWrap<R> implements Function4<R, P1, P2, P3, P4> {
		public Function4Impl(Class<? extends R> returnCls, Method method) {
			super(returnCls, method);
		}

		@Override
		public R call(Object target, P1 p1, P2 p2, P3 p3, P4 p4) {
			return super.call(target, p1, p2, p3);
		}
	}

	public static <R, P1, P2, P3, P4> Function4<R, P1, P2, P3, P4> create(Class<? extends R> returnCls, Class<?> target, Class<? extends P1> p1, Class<? extends P2> p2, Class<? extends P3> p3, Class<? extends P4> p4, String... names) {
		return new Function4Impl<R, P1, P2, P3, P4>(returnCls, ReflectionHelper.getMethod(target, names, p1, p2, p3, p4));
	}
}
