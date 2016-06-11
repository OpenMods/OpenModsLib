package openmods.serializable.cls;

import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import openmods.serializable.IObjectSerializer;
import openmods.utils.CachedFactory;
import openmods.utils.FieldsSelector;

public class ClassSerializersProvider {
	public static final ClassSerializersProvider instance = new ClassSerializersProvider();

	private final CachedFactory<Class<?>, IObjectSerializer<?>> cache = new CachedFactory<Class<?>, IObjectSerializer<?>>() {
		@Override
		protected IObjectSerializer<?> create(Class<?> key) {
			ClassSerializerBuilder<Object> builder = new ClassSerializerBuilder<Object>(key);

			for (Field f : SELECTOR.getFields(key))
				builder.appendField(f);

			return builder.create();
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

	@SuppressWarnings("unchecked")
	public <T> IObjectSerializer<T> getSerializer(Class<? extends T> cls) {
		return (IObjectSerializer<T>)cache.getOrCreate(cls);
	}

	@SuppressWarnings("unchecked")
	public <T> IObjectSerializer<T> getSerializer(T object) {
		return getSerializer((Class<? extends T>)object.getClass());
	}

	public void readFromStream(Object object, DataInput input) throws IOException {
		getSerializer(object).readFromStream(object, input);
	}

	public void writeToStream(Object object, DataOutput output) throws IOException {
		getSerializer(object).writeToStream(object, output);
	}

}