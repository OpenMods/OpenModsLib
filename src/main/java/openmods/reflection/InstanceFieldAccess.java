package openmods.reflection;

import java.lang.reflect.Field;


import com.google.common.base.Throwables;

public class InstanceFieldAccess<T> {

	public final Object target;
	public final Field field;

	public InstanceFieldAccess(Object parent, Field field) {
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
		return new InstanceFieldAccess<T>(target, f);
	}

	public static <T> InstanceFieldAccess<T> create(Class<?> cls, String... names) {
		return create(cls, null, names);
	}

	public static <T> InstanceFieldAccess<T> create(Object target, String... names) {
		return create(target.getClass(), target, names);
	}
}
