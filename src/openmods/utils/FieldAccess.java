package openmods.utils;

import java.lang.reflect.Field;

import com.google.common.base.Throwables;

public class FieldAccess<T> {

	public final T parent;
	public final Field field;

	public FieldAccess(T parent, Field field) {
		this.parent = parent;
		this.field = field;
	}

	public Object get() {
		try {
			return field.get(parent);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public void set(Object value) {
		try {
			field.set(parent, value);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}
}
