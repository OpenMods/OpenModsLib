package openmods.serializable.providers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;

import openmods.serializable.ISerializerProvider;
import openmods.serializable.SerializerRegistry;
import openmods.utils.ByteUtils;
import openmods.utils.io.*;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class ArraySerializerProvider implements ISerializerProvider {

	@Override
	public IStreamSerializer<?> getSerializer(Class<?> cls) {
		if (cls.isArray()) {
			final Class<?> componentCls = cls.getComponentType();
			return componentCls.isPrimitive()
					? createPrimitiveSerializer(componentCls)
					: createNullableSerializer(componentCls);
		}

		return null;
	}

	private static IStreamSerializer<?> createPrimitiveSerializer(final Class<?> componentCls) {
		final IStreamSerializer<Object> componentSerializer = SerializerRegistry.instance.findSerializer(componentCls);
		return new IStreamSerializer<Object>() {
			@Override
			public Object readFromStream(DataInput input) throws IOException {
				final int length = ByteUtils.readVLI(input);
				Object result = Array.newInstance(componentCls, length);

				for (int i = 0; i < length; i++) {
					final Object value = componentSerializer.readFromStream(input);
					Array.set(result, i, value);
				}

				return result;
			}

			@Override
			public void writeToStream(Object o, DataOutput output) throws IOException {
				final int length = Array.getLength(o);
				ByteUtils.writeVLI(output, length);

				for (int i = 0; i < length; i++) {
					Object value = Array.get(o, i);
					componentSerializer.writeToStream(value, output);
				}
			}
		};
	}

	private static IStreamSerializer<?> createNullableSerializer(final Class<?> componentCls) {
		final IStreamSerializer<Object> componentSerializer = SerializerRegistry.instance.findSerializer(componentCls);
		return new IStreamSerializer<Object>() {
			@Override
			public Object readFromStream(DataInput input) throws IOException {
				final int length = ByteUtils.readVLI(input);

				Object result = Array.newInstance(componentCls, length);

				if (length > 0) {
					final int nullBitsSize = StreamUtils.bitsToBytes(length);
					final byte[] nullBits = StreamUtils.readBytes(input, nullBitsSize);
					final InputBitStream nullBitStream = InputBitStream.create(nullBits);

					for (int i = 0; i < length; i++) {
						if (nullBitStream.readBit()) {
							final Object value = componentSerializer.readFromStream(input);
							Array.set(result, i, value);
						}
					}
				}

				return result;
			}

			@Override
			public void writeToStream(Object o, DataOutput output) throws IOException {
				final int length = Array.getLength(o);
				ByteUtils.writeVLI(output, length);

				if (length > 0) {
					final ByteArrayDataOutput nullBits = ByteStreams.newDataOutput();
					final OutputBitStream nullBitsStream = OutputBitStream.create(nullBits);

					for (int i = 0; i < length; i++) {
						Object value = Array.get(o, i);
						nullBitsStream.writeBit(value != null);
					}

					nullBitsStream.flush();
					output.write(nullBits.toByteArray());

					for (int i = 0; i < length; i++) {
						Object value = Array.get(o, i);
						if (value != null) componentSerializer.writeToStream(value, output);
					}
				}
			}
		};
	}
}
