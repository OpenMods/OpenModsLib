package openmods.network.rpc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Map;

import openmods.utils.AnnotationMap;
import openmods.utils.ByteUtils;
import openmods.utils.io.IStreamReadable;
import openmods.utils.io.IStreamWriteable;
import openmods.utils.io.TypeRW;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class MethodParamsCodec {

	private static class MethodParam {
		public final Class<?> type;

		public final boolean isNullable;

		public MethodParam(Class<?> type, Annotation[] annotations) {
			this.type = type;
			AnnotationMap annotationsMap = new AnnotationMap(annotations);
			isNullable = annotationsMap.hasAnnotation(NullableArg.class);
		}

		public void validate() {
			validate(type);
		}

		private void validate(Class<?> cls) {
			Preconditions.checkState(!cls.isPrimitive() || !isNullable, "Primitive types can't be nullable");

			if (type.isArray()) validate(type.getComponentType());
			else if (type.isEnum()) {
				// NO-OP, enums are always valid, unless...
				// TODO: size validation? is that even possible?
			} else {
				IStreamReadable<?> reader = TypeRW.TYPES.get(type);
				Preconditions.checkNotNull(reader, "Failed to find reader for type %s", type);
			}
		}

		@Override
		public String toString() {
			return "MethodParam [type=" + type + ", nullable=" + isNullable + "]";
		}
	}

	private final Method method;

	private final MethodParam[] params;

	public MethodParamsCodec(Method method) {
		this.method = method;

		Annotation[][] annotations = method.getParameterAnnotations();
		Class<?>[] types = method.getParameterTypes();

		params = new MethodParam[types.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = new MethodParam(types[i], annotations[i]);
		}
	}

	public void writeArgs(DataOutput output, Object... args) {
		if (args == null) {
			Preconditions.checkArgument(0 == params.length,
					"Argument list length mismatch, expected %d, got 0", params.length);
			return;
		}

		Preconditions.checkArgument(args.length == params.length,
				"Argument list length mismatch, expected %d, got %d", params.length, args.length);
		for (int i = 0; i < args.length; i++) {
			MethodParam param = params[i];
			try {
				writeArg(output, i, param.type, param.isNullable, args[i]);
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to write argument %d from method %s", i, method), e);
			}
		}
	}

	private static void writeArg(DataOutput output, int argIndex, Class<?> type, boolean isNullable, Object value) throws IOException {
		if (isNullable) {
			if (value == null) {
				output.writeBoolean(false);
				return;
			}
			output.writeBoolean(true);
		} else {
			Preconditions.checkNotNull(value, "Only @NullableArg arguments can be null");
		}

		if (type.isArray()) {
			writeArray(output, argIndex, type, isNullable, value);
		} else if (type.isEnum()) {
			writeEnum(output, value);
		} else {
			writeSingleValue(output, type, value);
		}
	}

	private static void writeEnum(DataOutput output, Object value) {
		int ord = ((Enum<?>)value).ordinal();
		ByteUtils.writeVLI(output, ord);
	}

	private static void writeArray(DataOutput output, int argIndex, Class<?> type, boolean isNullable, Object value) throws IOException {
		final int length = Array.getLength(value);
		final Class<?> component = type.getComponentType();
		ByteUtils.writeVLI(output, length);
		for (int i = 0; i < length; i++) {
			Object elem = Array.get(value, i);
			writeArg(output, argIndex, component, isNullable, elem);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void writeSingleValue(DataOutput output, final Class<?> type, Object value) throws IOException {
		IStreamWriteable writer = TypeRW.TYPES.get(type);
		Preconditions.checkNotNull(writer, "Failed to find writer for type %s", type);
		writer.writeToStream(value, output);
	}

	public Object[] readArgs(DataInput input) {
		if (params.length == 0) return null;

		Object[] result = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			MethodParam param = params[i];
			try {
				result[i] = readArg(input, param.type, param.isNullable);
			} catch (Exception e) {
				throw new RuntimeException(String.format("Failed to read argument %d from method %s", i, method), e);
			}
		}

		return result;
	}

	private static Object readArg(DataInput input, Class<?> type, boolean isNullable) throws IOException {
		if (isNullable) {
			boolean hasValue = input.readBoolean();
			if (!hasValue) return null;
		}

		if (type.isArray()) {
			return readArray(input, type, isNullable);
		} else if (type.isEnum()) {
			return readEnum(input, type);
		} else {
			return readSingleValue(input, type);
		}
	}

	private static Object readArray(DataInput input, Class<?> type, boolean isNullable) throws IOException {
		Class<?> component = type.getComponentType();
		int length = ByteUtils.readVLI(input);
		Object result = Array.newInstance(component, length);
		for (int i = 0; i < length; i++) {
			Object value = readArg(input, component, isNullable);
			Array.set(result, i, value);
		}
		return result;
	}

	private static Object readEnum(DataInput input, Class<?> type) {
		int ord = ByteUtils.readVLI(input);
		Object[] values = type.getEnumConstants();
		try {
			return values[ord];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException(String.format("Failed to get enum with ordinal %d from class %s", ord, type));
		}
	}

	private static Object readSingleValue(DataInput input, Class<?> type) throws IOException {
		IStreamReadable<?> reader = TypeRW.TYPES.get(type);
		Preconditions.checkNotNull(reader, "Failed to find reader for type %s", type);
		return reader.readFromStream(input);
	}

	public void validate() {
		for (int i = 0; i < params.length; i++) {
			try {
				params[i].validate();
			} catch (Exception e) {
				throw new IllegalStateException(String.format("Failed to validate arg %d of method %s", i, method), e);
			}
		}
	}

	private static final Map<Method, MethodParamsCodec> CACHE = Maps.newHashMap();

	public static synchronized MethodParamsCodec create(Method method) {
		MethodParamsCodec result = CACHE.get(method);

		if (result == null) {
			result = new MethodParamsCodec(method);
			CACHE.put(method, result);
		}

		return result;
	}
}
