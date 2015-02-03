package openmods.serializable.cls;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import openmods.serializable.IObjectSerializer;
import openmods.utils.CachedFactory;
import openmods.utils.FieldsSelector;

import com.google.common.collect.Lists;

public class ClassSerializer<T> extends CachedFactory<Class<? extends T>, IObjectSerializer<T>> implements IObjectSerializer<T> {
	private final FieldsSelector SELECTOR = new FieldsSelector() {
		@Override
		protected boolean shouldInclude(Field field) {
			return field.isAnnotationPresent(Serialize.class);
		}

		@Override
		protected Field[] listFields(Class<?> cls) {
			return cls.getFields();
		}
	};

	public static <T> IObjectSerializer<T> createFieldsSerializer(Iterable<Field> fields) {
		List<IObjectSerializer<T>> result = Lists.newArrayList();

		for (Field f : fields)
			result.add(new SerializableField<T>(f));

		return new ComposedSerializer<T>(result);
	}

	@Override
	protected IObjectSerializer<T> create(Class<? extends T> key) {
		final Collection<Field> fields = SELECTOR.getFields(key);
		return createFieldsSerializer(fields);
	}

	@SuppressWarnings("unchecked")
	public IObjectSerializer<T> getSerializer(T object) {
		return getOrCreate((Class<? extends T>)object.getClass());
	}

	@Override
	public void readFromStream(T object, DataInput input) throws IOException {
		getSerializer(object).readFromStream(object, input);
	}

	@Override
	public void writeToStream(T object, DataOutput output) throws IOException {
		getSerializer(object).writeToStream(object, output);
	}

}