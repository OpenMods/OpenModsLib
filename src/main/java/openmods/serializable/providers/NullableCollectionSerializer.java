package openmods.serializable.providers;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.reflect.TypeToken;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Type;
import openmods.serializable.SerializerRegistry;
import openmods.utils.ByteUtils;
import openmods.utils.io.IStreamSerializer;
import openmods.utils.io.InputBitStream;
import openmods.utils.io.OutputBitStream;
import openmods.utils.io.StreamUtils;

public abstract class NullableCollectionSerializer<T> implements IStreamSerializer<T> {

	public static IStreamSerializer<Object[]> createObjectArraySerializer(final TypeToken<?> componentType) {
		return new NullableCollectionSerializer<Object[]>(componentType) {

			@Override
			protected Object[] createCollection(TypeToken<?> componentCls, int length) {
				return new Object[length];
			}

			@Override
			protected int getLength(Object[] collection) {
				return collection.length;
			}

			@Override
			protected Object getElement(Object[] collection, int index) {
				return collection[index];
			}

			@Override
			protected void setElement(Object[] collection, int index, Object value) {
				collection[index] = value;
			}
		};
	}

	private final IStreamSerializer<Object> componentSerializer;
	private final TypeToken<?> componentType;

	public NullableCollectionSerializer(TypeToken<?> componentType) {
		final Type type = componentType.getType();
		this.componentSerializer = SerializerRegistry.instance.findSerializer(type);
		Preconditions.checkNotNull(componentSerializer, "Can't find serializer for %s", type);
		this.componentType = componentType;
	}

	@Override
	public T readFromStream(DataInput input) throws IOException {
		final int length = ByteUtils.readVLI(input);

		T result = createCollection(componentType, length);

		if (length > 0) {
			final int nullBitsSize = StreamUtils.bitsToBytes(length);
			final byte[] nullBits = StreamUtils.readBytes(input, nullBitsSize);
			final InputBitStream nullBitStream = InputBitStream.create(nullBits);

			for (int i = 0; i < length; i++) {
				if (nullBitStream.readBit()) {
					final Object value = componentSerializer.readFromStream(input);
					setElement(result, i, value);
				}
			}
		}

		return result;
	}

	@Override
	public void writeToStream(T o, DataOutput output) throws IOException {
		final int length = getLength(o);
		ByteUtils.writeVLI(output, length);

		if (length > 0) {
			final ByteArrayDataOutput nullBits = ByteStreams.newDataOutput();
			final OutputBitStream nullBitsStream = OutputBitStream.create(nullBits);

			for (int i = 0; i < length; i++) {
				Object value = getElement(o, i);
				nullBitsStream.writeBit(value != null);
			}

			nullBitsStream.flush();
			output.write(nullBits.toByteArray());

			for (int i = 0; i < length; i++) {
				Object value = getElement(o, i);
				if (value != null) componentSerializer.writeToStream(value, output);
			}
		}
	}

	protected abstract T createCollection(TypeToken<?> componentCls, int length);

	protected abstract int getLength(T collection);

	protected abstract Object getElement(T collection, int index);

	protected abstract void setElement(T collection, int index, final Object value);

}
