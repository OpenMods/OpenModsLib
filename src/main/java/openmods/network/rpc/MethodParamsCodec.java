package openmods.network.rpc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import openmods.serializable.SerializerRegistry;
import openmods.utils.AnnotationMap;
import openmods.utils.CachedFactory;
import openmods.utils.io.IStreamReader;

import com.google.common.base.Preconditions;

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
			else {
				IStreamReader<?> reader = SerializerRegistry.instance.findSerializer(cls);
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

	MethodParamsCodec(Method method) {
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

		SerializerRegistry.instance.writeToStream(output, value);
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

		return SerializerRegistry.instance.createFromStream(input, type);
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

	private static final CachedFactory<Method, MethodParamsCodec> INSTANCES = new CachedFactory<Method, MethodParamsCodec>() {
		@Override
		protected MethodParamsCodec create(Method key) {
			return new MethodParamsCodec(key);
		}
	};

	public static synchronized MethodParamsCodec create(Method method) {
		return INSTANCES.getOrCreate(method);
	}
}
