package openmods.utils;

import java.lang.reflect.Field;
import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public abstract class FieldsSelector {

	private static final Comparator<Field> FIELD_NAME_COMPARATOR = new Comparator<Field>() {
		@Override
		public int compare(Field o1, Field o2) {
			// No need to worry about nulls
			return o1.getName().compareTo(o2.getName());
		}
	};

	private final Map<Class<?>, Collection<Field>> syncedFields = Maps.newIdentityHashMap();

	protected abstract boolean shouldInclude(Field field);

	protected abstract Field[] listFields(Class<?> cls);

	private Collection<Field> scanForFields(Class<?> cls) {
		Set<Field> fields = Sets.newTreeSet(FIELD_NAME_COMPARATOR);
		for (Field field : listFields(cls)) {
			if (shouldInclude(field)) {
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return ImmutableList.copyOf(fields);
	}

	public Collection<Field> getFields(Class<?> cls) {

		Collection<Field> result;
		synchronized (syncedFields) {
			result = syncedFields.get(cls);
		}

		if (result == null) {
			result = scanForFields(cls);

			synchronized (syncedFields) {
				syncedFields.put(cls, result);
			}
		}

		return result;
	}
}
