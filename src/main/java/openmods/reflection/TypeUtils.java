package openmods.reflection;

import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.reflect.TypeToken;

public class TypeUtils {

	public static final TypeVariable<?> LIST_VALUE_PARAM;

	public static final TypeVariable<?> MAP_KEY_PARAM;

	public static final TypeVariable<?> MAP_VALUE_PARAM;

	static {
		TypeVariable<?>[] vars = Map.class.getTypeParameters();
		MAP_KEY_PARAM = vars[0];
		MAP_VALUE_PARAM = vars[1];

		LIST_VALUE_PARAM = List.class.getTypeParameters()[0];
	}

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

	public static void isInstance(Object o, Class<?> mainCls, Class<?>... extraCls) {
		Preconditions.checkArgument(mainCls.isInstance(o), "%s is not instance of %s", o, mainCls);
		for (Class<?> cls : extraCls)
			Preconditions.checkArgument(cls.isInstance(o), "%s is not instance of %s", o, cls);
	}

	public static boolean compareTypes(Class<?> left, Class<?> right) {
		if (left.isPrimitive()) left = PRIMITIVE_TYPES_MAP.get(left);
		if (right.isPrimitive()) right = PRIMITIVE_TYPES_MAP.get(right);
		return left.equals(right);
	}

	public static TypeToken<?> getTypeParameter(Class<?> intfClass, Class<?> instanceClass, int index) {
		final TypeVariable<?>[] typeParameters = intfClass.getTypeParameters();
		Preconditions.checkElementIndex(index, typeParameters.length, intfClass + " type parameter index");
		TypeVariable<?> arg = typeParameters[index];
		TypeToken<?> type = TypeToken.of(instanceClass);
		Preconditions.checkArgument(type.getRawType() != Object.class, "Type %s is no fully parametrized", instanceClass);
		return type.resolveType(arg);
	}

	public static TypeToken<?> getTypeParameter(Class<?> intfClass, Class<?> instanceClass) {
		return getTypeParameter(intfClass, instanceClass, 0);
	}

}
