package openmods.config.simpler;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import openmods.reflection.FieldAccess;
import openmods.utils.CachedFactory;
import openmods.utils.io.IStringSerializer;
import openmods.utils.io.TypeRW;

public class ConfigurableClassAdapter<T> {

	public static class NoSuchPropertyException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public NoSuchPropertyException(String message) {
			super(message);
		}
	}

	private static class FieldAdapter<T> {

		private final IStringSerializer<T> serializer;

		private final FieldAccess<T> access;

		public FieldAdapter(IStringSerializer<T> serializer, FieldAccess<T> access) {
			this.serializer = serializer;
			this.access = access;
		}

		public void set(Object instance, String value) {
			T converted = serializer.readFromString(value);
			access.set(instance, converted);
		}

		public String get(Object instance) {
			T value = access.get(instance);
			return String.valueOf(value);
		}
	}

	private final Class<? extends T> cls;

	private final Map<String, FieldAdapter<?>> fields;

	public ConfigurableClassAdapter(Class<? extends T> cls) {
		this.cls = cls;

		ImmutableMap.Builder<String, FieldAdapter<?>> fields = ImmutableMap.builder();
		for (Field f : cls.getFields()) {
			Configurable ann = f.getAnnotation(Configurable.class);
			if (ann != null) {
				String name = ann.name();
				if (name.isEmpty()) name = f.getName();

				final FieldAccess<?> access = FieldAccess.create(f);
				final IStringSerializer<?> serializer = TypeRW.getStringSerializer(f.getType());
				Preconditions.checkState(serializer != null, "Can't find serializer for field %s", f);

				@SuppressWarnings({ "rawtypes", "unchecked" })
				final FieldAdapter<?> adapter = new FieldAdapter(serializer, access);

				fields.put(name, adapter);
			}
		}

		this.fields = fields.build();
	}

	public Set<String> keys() {
		return fields.keySet();
	}

	private FieldAdapter<?> findField(String key) {
		final FieldAdapter<?> fieldAdapter = fields.get(key);
		if (fieldAdapter == null) throw new NoSuchPropertyException(String.format("Can't find key %s in class %s", key, cls));
		return fieldAdapter;
	}

	public String get(T instance, String key) {
		return findField(key).get(instance);
	}

	public void set(T instance, String key, String value) {
		findField(key).set(instance, value);
	}

	private static final CachedFactory<Class<?>, ConfigurableClassAdapter<?>> CACHE = new CachedFactory<Class<?>, ConfigurableClassAdapter<?>>() {
		@Override
		protected ConfigurableClassAdapter<?> create(Class<?> key) {
			return new ConfigurableClassAdapter<Object>(key);
		}
	};

	@SuppressWarnings("unchecked")
	public static <T> ConfigurableClassAdapter<T> getFor(Class<? extends T> cls) {
		return (ConfigurableClassAdapter<T>)CACHE.getOrCreate(cls);
	}
}
