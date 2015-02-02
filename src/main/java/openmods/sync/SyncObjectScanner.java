package openmods.sync;

import java.lang.reflect.Field;

import openmods.Log;
import openmods.utils.FieldsSelector;

import com.google.common.base.Preconditions;

public class SyncObjectScanner extends FieldsSelector {
	public static final SyncObjectScanner INSTANCE = new SyncObjectScanner();

	@Override
	protected boolean shouldInclude(Field field) {
		return ISyncableObject.class.isAssignableFrom(field.getType());
	}

	@Override
	protected Field[] listFields(Class<?> cls) {
		return cls.getDeclaredFields();
	}

	public void registerAllFields(SyncMap<?> map, Object target) {
		for (Field field : getFields(target.getClass())) {
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
