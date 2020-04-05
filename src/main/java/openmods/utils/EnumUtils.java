package openmods.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class EnumUtils {

	private static class EnumMappings<T extends Enum<T>> {
		private final Int2ObjectMap<T> idToValue = new Int2ObjectOpenHashMap<>();

		public EnumMappings(Class<T> enumCls) {

			for (T value : enumCls.getEnumConstants())
				idToValue.put(value.ordinal(), value);
		}

		public T getValue(int id) {
			return idToValue.get(id);
		}

	}

	private static final CachedFactory<Class<? extends Enum<?>>, EnumMappings<?>> mappings = new CachedFactory<Class<? extends Enum<?>>, EnumMappings<?>>() {
		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		protected EnumMappings<?> create(Class<? extends Enum<?>> key) {
			return new EnumMappings(key);
		}
	};

	public static <T extends Enum<T>> T fromOrdinal(Class<T> cls, int ordinal) {
		@SuppressWarnings("unchecked")
		final EnumMappings<T> mapping = (EnumMappings<T>)mappings.getOrCreate(cls);
		return mapping.getValue(ordinal);
	}
}
