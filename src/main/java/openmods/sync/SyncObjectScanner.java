package openmods.sync;

import java.lang.reflect.Field;
import java.util.List;

import openmods.Log;
import openmods.utils.FieldsSelector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class SyncObjectScanner extends FieldsSelector {
	public static final SyncObjectScanner INSTANCE = new SyncObjectScanner();

	@Override
	protected List<FieldEntry> listFields(Class<?> cls) {
		List<FieldEntry> result = Lists.newArrayList();
		for (Field f : cls.getDeclaredFields())
			if (ISyncableObject.class.isAssignableFrom(f.getType())) result.add(new FieldEntry(f, 0));

		return result;
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
