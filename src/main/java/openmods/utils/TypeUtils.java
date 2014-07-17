package openmods.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class TypeUtils {

	public static final BiMap<Class<?>, Class<?>> PRIMITIVE_TYPES_MAP = ImmutableBiMap.<Class<?>, Class<?>> builder()
			.put(boolean.class, Boolean.class)
			.put(byte.class, Byte.class)
			.put(char.class, Character.class)
			.put(short.class, Short.class)
			.put(int.class, Integer.class)
			.put(long.class, Long.class)
			.put(float.class, Float.class)
			.put(double.class, Double.class)
			.put(void.class, Void.class)
			.build();

	public static Class<?> toObjectType(Class<?> cls) {
		return cls.isPrimitive()? PRIMITIVE_TYPES_MAP.get(cls) : cls;
	}

}
