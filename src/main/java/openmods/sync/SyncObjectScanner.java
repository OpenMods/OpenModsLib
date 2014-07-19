package openmods.sync;

import java.lang.reflect.Field;
import java.util.*;

import openmods.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SyncObjectScanner {
	public static final SyncObjectScanner INSTANCE = new SyncObjectScanner();

	private static final Comparator<Field> FIELD_NAME_COMPARATOR = new Comparator<Field>() {
		@Override
		public int compare(Field o1, Field o2) {
			// No need to worry about nulls
			return o1.getName().compareTo(o2.getName());
		}
	};

	private final Map<Class<?>, Collection<Field>> syncedFields = Maps.newIdentityHashMap();

	private static Collection<Field> scanForFields(Class<?> cls) {
		Set<Field> fields = Sets.newTreeSet(FIELD_NAME_COMPARATOR);
		for (Field field : cls.getDeclaredFields()) {
			if (ISyncableObject.class.isAssignableFrom(field.getType())) {
				fields.add(field);
				field.setAccessible(true);
			}
		}
		return ImmutableList.copyOf(fields);
	}

	private Collection<Field> getSyncedFields(Class<?> cls) {

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

	public void registerAllFields(SyncMap<?> map, Object target) {
		for (Field field : getSyncedFields(target.getClass())) {
			ISyncableObject obj;
			try {
				obj = (ISyncableObject)field.get(target);
				Preconditions.checkNotNull(obj, "Null field value");
			} catch (Exception e) {
				obj = DummySyncableObject.INSTANCE;
				Log.severe(e, "Exception while registering synced field '%s' of object '%s'", field, target);
			}

			final String fieldName = field.getName();
			map.put(fieldName, obj);
		}
	}
}
