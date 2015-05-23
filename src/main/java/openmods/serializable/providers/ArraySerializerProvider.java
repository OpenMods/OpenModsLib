package openmods.serializable.providers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;

import openmods.serializable.ISerializerProvider;
import openmods.serializable.SerializerRegistry;
import openmods.utils.ByteUtils;
import openmods.utils.io.IStreamSerializer;

public class ArraySerializerProvider implements ISerializerProvider {

	@Override
	public IStreamSerializer<?> getSerializer(Class<?> cls) {
		return cls.isArray()? createSerializer(cls) : null;
	}

	private static IStreamSerializer<?> createSerializer(Class<?> cls) {
		final Class<?> componentCls = cls.getComponentType();
		final IStreamSerializer<Object> componentSerializer = SerializerRegistry.instance.findSerializer(componentCls);

		return new IStreamSerializer<Object>() {
			@Override
			public Object readFromStream(DataInput input) throws IOException {
				int length = ByteUtils.readVLI(input);
				Object result = Array.newInstance(componentCls, length);

				for (int i = 0; i < length; i++) {
					final Object value = componentSerializer.readFromStream(input);
					Array.set(result, i, value);
				}

				return result;
			}

			@Override
			public void writeToStream(Object o, DataOutput output) throws IOException {
				int size = Array.getLength(o);
				ByteUtils.writeVLI(output, size);

				for (int i = 0; i < size; i++) {
					Object value = Array.get(o, i);
					componentSerializer.writeToStream(value, output);
				}
			}
		};
	}

}
