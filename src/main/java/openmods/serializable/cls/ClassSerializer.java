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

public class ClassSerializer {
	public static final ClassSerializer instance = new ClassSerializer();

	private final CachedFactory<Class<?>, IObjectSerializer<?>> cache = new CachedFactory<Class<?>, IObjectSerializer<?>>() {

		@Override
		protected IObjectSerializer<?> create(Class<?> key) {
			final Collection<Field> fields = SELECTOR.getFields(key);
			return createFieldsSerializer(fields);
		}
	};

	private final FieldsSelector SELECTOR = new FieldsSelector() {

		@Override
		protected List<FieldEntry> listFields(Class<?> cls) {
			List<FieldEntry> result = Lists.newArrayList();
			for (Field f : cls.getFields()) {
				Serialize ann = f.getAnnotation(Serialize.class);
				if (ann != null) result.add(new FieldEntry(f, ann.rank()));
			}

			return result;
		}
	};

	public static <T> IObjectSerializer<T> createFieldsSerializer(Iterable<Field> fields) {
		List<IObjectSerializer<T>> result = Lists.newArrayList();

		for (Field f : fields)
			result.add(new SerializableField<T>(f));

		return new ComposedSerializer<T>(result);
	}

	@SuppressWarnings("unchecked")
	public <T> IObjectSerializer<T> getSerializer(T object) {
		return (IObjectSerializer<T>)cache.getOrCreate(object.getClass());
	}

	public void readFromStream(Object object, DataInput input) throws IOException {
		getSerializer(object).readFromStream(object, input);
	}

	public void writeToStream(Object object, DataOutput output) throws IOException {
		getSerializer(object).writeToStream(object, output);
	}

}