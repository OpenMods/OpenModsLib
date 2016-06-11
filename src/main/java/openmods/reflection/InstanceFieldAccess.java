package openmods.reflection;

import com.google.common.base.Throwables;
import java.lang.reflect.Field;

public class InstanceFieldAccess<T> {

	public final Object target;
	public final Field field;

	public InstanceFieldAccess(Object parent, Field field) {
		this(parent, field, true);
	}

	private InstanceFieldAccess(Object parent, Field field, boolean log) {
		if (log) ReflectionLog.logLoad(field);
		this.target = parent;
		this.field = field;
		field.setAccessible(true);
	}

	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T)field.get(target);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public void set(T value) {
		try {
			field.set(target, value);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static <T> InstanceFieldAccess<T> create(Class<?> cls, Object target, String... names) {
		Field f = ReflectionHelper.getField(cls, names);
		return new InstanceFieldAccess<T>(target, f, false); // log done in ReflectionHelper
	}

	public static <T> InstanceFieldAccess<T> create(Class<?> cls, String... names) {
		return create(cls, null, names);
	}

	public static <T> InstanceFieldAccess<T> create(Object target, String... names) {
		return create(target.getClass(), target, names);
	}
}
