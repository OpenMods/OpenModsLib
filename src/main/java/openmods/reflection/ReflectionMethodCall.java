package openmods.reflection;

import java.lang.reflect.Method;
import java.util.Arrays;

import openmods.Log;

public class ReflectionMethodCall<T> {

	private final Method method;

	public ReflectionMethodCall(Class<?> targetCls, String[] names, Class<?>... args) {
		Method method = null;
		try {
			method = ReflectionHelper.getMethod(targetCls, names, args);
			method.setAccessible(true);
		} catch (Throwable t) {
			Log.warn(t, "Failed to get method '%s.%s(%s)'", targetCls, Arrays.toString(names), Arrays.toString(args));
		}

		this.method = method;
	}

	@SuppressWarnings("unchecked")
	public T call(Object target, Object... args) {
		if (method != null) {
			try {
				return (T)method.invoke(target, args);
			} catch (Throwable t) {
				Log.warn(t, "Can't call method. Oh, well...");
			}
		}

		return null;
	}

}
