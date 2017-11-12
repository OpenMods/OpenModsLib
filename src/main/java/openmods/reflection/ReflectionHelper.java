package openmods.reflection;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import openmods.Log;
import openmods.utils.SneakyThrower;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Level;

public class ReflectionHelper {

	public static class MethodNotFound extends RuntimeException {
		private static final long serialVersionUID = 4931028064550261584L;

		private MethodNotFound(Class<?> cls, String[] names, Class<?>[] args) {
			super("Method not found: " + cls + "." + Arrays.toString(names) + Arrays.toString(args));
		}
	}

	public static class FieldNotFound extends RuntimeException {
		private static final long serialVersionUID = 6119776323412256889L;

		private FieldNotFound(Class<?> cls, String... names) {
			super("Field not found: " + cls + "." + Arrays.toString(names));
		}
	}

	private static class NullMarker {
		public final Class<?> cls;

		private NullMarker(Class<?> cls) {
			this.cls = cls;
		}
	}

	private static class TypeMarker {
		public final Class<?> cls;
		public final Object value;

		private TypeMarker(Class<?> cls, Object value) {
			this.cls = cls;
			this.value = value;
		}
	}

	public static Object nullValue(Class<?> cls) {
		return new NullMarker(cls);
	}

	public static Object typed(Object value, Class<?> cls) {
		return new TypeMarker(cls, value);
	}

	public static Object primitive(char value) {
		return new TypeMarker(char.class, value);
	}

	public static Object primitive(long value) {
		return new TypeMarker(long.class, value);
	}

	public static Object primitive(int value) {
		return new TypeMarker(int.class, value);
	}

	public static Object primitive(short value) {
		return new TypeMarker(short.class, value);
	}

	public static Object primitive(byte value) {
		return new TypeMarker(byte.class, value);
	}

	public static Object primitive(float value) {
		return new TypeMarker(float.class, value);
	}

	public static Object primitive(double value) {
		return new TypeMarker(double.class, value);
	}

	public static Object primitive(boolean value) {
		return new TypeMarker(boolean.class, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getProperty(Class<?> klazz, Object instance, String... fields) {
		Field field = getField(klazz == null? instance.getClass() : klazz, fields);

		try {
			return (T)field.get(instance);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T getProperty(String className, Object instance, String... fields) {
		return getProperty(getClass(className), instance, fields);
	}

	public static <T> T getProperty(Object instance, String... fields) {
		return getProperty(instance.getClass(), instance, fields);
	}

	public static void setProperty(Class<?> klazz, Object instance, Object value, String... fields) {
		Field field = getField(klazz == null? instance.getClass() : klazz, fields);

		try {
			field.set(instance, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static void setProperty(String className, Object instance, Object value, String... fields) {
		setProperty(getClass(className), instance, value, fields);
	}

	public static void setProperty(Object instance, Object value, String... fields) {
		setProperty(instance.getClass(), instance, value, fields);
	}

	public static <T> T callStatic(Class<?> klazz, String methodName, Object... args) {
		return call(klazz, null, ArrayUtils.toArray(methodName), args);
	}

	public static <T> T call(Object instance, String methodName, Object... args) {
		return call(instance.getClass(), instance, ArrayUtils.toArray(methodName), args);
	}

	public static <T> T call(Object instance, String[] methodNames, Object... args) {
		return call(instance.getClass(), instance, methodNames, args);
	}

	public static <T> T call(Class<?> cls, Object instance, String methodName, Object... args) {
		return call(cls, instance, ArrayUtils.toArray(methodName), args);
	}

	@SuppressWarnings("unchecked")
	public static <T> T call(Class<?> klazz, Object instance, String[] methodNames, Object... args) {
		Method m = getMethod(klazz, methodNames, args);

		for (int i = 0; i < args.length; i++) {
			final Object arg = args[i];
			if (arg instanceof NullMarker) args[i] = null;
			if (arg instanceof TypeMarker) args[i] = ((TypeMarker)arg).value;
		}

		m.setAccessible(true);
		try {
			return (T)m.invoke(instance, args);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static Method getMethod(Class<?> klazz, String[] methodNames, Object... args) {
		if (klazz == null) return null;
		Class<?> argTypes[] = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			final Object arg = args[i];
			Preconditions.checkNotNull(arg, "No nulls allowed, use wrapper types");
			if (arg instanceof NullMarker) argTypes[i] = ((NullMarker)arg).cls;
			else if (arg instanceof TypeMarker) argTypes[i] = ((TypeMarker)arg).cls;
			else argTypes[i] = arg.getClass();
		}

		for (String name : methodNames) {
			Method result = getDeclaredMethod(klazz, name, argTypes);
			if (result != null) return result;
		}

		throw new MethodNotFound(klazz, methodNames, argTypes);
	}

	public static Method getMethod(Class<?> klazz, String[] methodNames, Class<?>... types) {
		if (klazz == null) return null;
		for (String name : methodNames) {
			Method result = getDeclaredMethod(klazz, name, types);
			if (result != null) return result;
		}

		throw new MethodNotFound(klazz, methodNames, types);
	}

	public static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
		ImmutableSet.Builder<Class<?>> result = ImmutableSet.builder();

		Class<?> current = clazz;
		while (current != null) {
			result.add(current.getInterfaces());
			current = current.getSuperclass();
		}

		return result.build();
	}

	public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>[] argsTypes) {
		while (clazz != null) {
			try {
				final Method m = clazz.getDeclaredMethod(name, argsTypes);
				ReflectionLog.logLoad(m);
				return m;
			} catch (NoSuchMethodException e) {}
			clazz = clazz.getSuperclass();
		}
		return null;
	}

	public static List<Method> getAllMethods(Class<?> clazz) {
		List<Method> methods = Lists.newArrayList();
		while (clazz != null) {
			for (Method m : clazz.getDeclaredMethods()) {
				methods.add(m);
				ReflectionLog.logLoad(m);
			}
			clazz = clazz.getSuperclass();
		}
		return methods;

	}

	public static Field getField(Class<?> klazz, String... fields) {
		for (String field : fields) {
			Class<?> current = klazz;
			while (current != null) {
				try {
					final Field f = current.getDeclaredField(field);
					f.setAccessible(true);
					ReflectionLog.logLoad(f);
					return f;
				} catch (NoSuchFieldException e) {}
				current = current.getSuperclass();
			}
		}

		throw new FieldNotFound(klazz, fields);
	}

	public static Class<?> getClass(String className) {
		if (Strings.isNullOrEmpty(className)) return null;
		try {
			final Class<?> cls = Class.forName(className);
			ReflectionLog.logLoad(cls);
			return cls;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static Class<?> getClass(String... classNames) {
		for (String className : classNames) {
			Preconditions.checkNotNull(className);
			try {
				final Class<?> cls = Class.forName(className);
				ReflectionLog.logLoad(cls);
				return cls;
			} catch (ClassNotFoundException e) {
				Log.log(Level.DEBUG, e, "Class %s not found", className);
			}
		}

		throw SneakyThrower.sneakyThrow(new ClassNotFoundException(Arrays.toString(classNames)));
	}

}
