package openmods.utils;

import java.lang.reflect.Field;

import com.google.common.base.Throwables;

public class FieldAccess<T> {

	public final Field field;

	public FieldAccess(Field field) {
		this.field = field;
		field.setAccessible(true);
	}

	@SuppressWarnings("unchecked")
	public T get(Object target) {
		try {
			return (T)field.get(target);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public void set(Object target, T value) {
		try {
			field.set(target, value);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static <T> FieldAccess<T> create(Class<?> cls, String... names) {
		Field f = ReflectionHelper.getField(cls, names);
		return new FieldAccess<T>(f);
	}
}
