package openmods.serializable.cls;

import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import net.minecraft.network.PacketBuffer;
import openmods.reflection.FieldAccess;
import openmods.reflection.TypeUtils;
import openmods.serializable.IObjectSerializer;
import openmods.serializable.SerializerRegistry;
import openmods.utils.io.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

public class ClassSerializerBuilder<T> {

	private static class SerializableField extends FieldAccess<Object> {
		private final IStreamSerializer<Object> serializer;
		private final boolean isNullable;

		public SerializableField(Class<?> ownerCls, Field field, boolean isNullable) {
			super(field);

			this.isNullable = isNullable;

			final TypeToken<?> fieldType = TypeUtils.resolveFieldType(ownerCls, field);
			this.serializer = SerializerRegistry.instance.findSerializer(fieldType.getType());
			Preconditions.checkNotNull(serializer, "Invalid field %s type", field);
		}
	}

	private static class NonNullableSerializer<T> implements IObjectSerializer<T> {

		private final List<SerializableField> fields;

		public NonNullableSerializer(List<SerializableField> fields) {
			this.fields = ImmutableList.copyOf(fields);
		}

		@Override
		public void readFromStream(T object, PacketBuffer input) throws IOException {
			for (SerializableField field : fields) {
				Object value = field.serializer.readFromStream(input);
				field.set(object, value);
			}
		}

		@Override
		public void writeToStream(T object, PacketBuffer output) throws IOException {
			for (SerializableField field : fields) {
				Object value = field.get(object);
				Preconditions.checkNotNull(value, "Non-nullable %s has null value", field.field);
				field.serializer.writeToStream(value, output);
			}
		}
	}

	private static class NullableSerializer<T> implements IObjectSerializer<T> {

		private final List<SerializableField> fields;

		private final int nullBytesCount;

		public NullableSerializer(List<SerializableField> fields, int nullBytesCount) {
			this.fields = ImmutableList.copyOf(fields);
			this.nullBytesCount = nullBytesCount;
		}

		@Override
		public void readFromStream(T object, PacketBuffer input) throws IOException {
			final byte[] nullBits = StreamUtils.readBytes(input, nullBytesCount);
			final InputBitStream nullBitStream = InputBitStream.create(nullBits);

			for (SerializableField field : fields) {
				final boolean isNull = field.isNullable && nullBitStream.readBit();
				final Object value = isNull? null : field.serializer.readFromStream(input);
				field.set(object, value);
			}
		}

		@Override
		public void writeToStream(T object, PacketBuffer output) throws IOException {
			final OutputBitStream nullBitsStream = OutputBitStream.create(output);

			final PacketBuffer payload = new PacketBuffer(Unpooled.buffer());
			for (SerializableField field : fields) {
				final Object value = field.get(object);
				if (field.isNullable) {
					if (value == null) {
						nullBitsStream.writeBit(true);
					} else {
						nullBitsStream.writeBit(false);
						field.serializer.writeToStream(value, payload);
					}
				} else {
					field.serializer.writeToStream(value, payload);
				}
			}

			nullBitsStream.flush();
			output.writeBytes(payload);
		}
	}

	private final Class<? extends T> ownerClass;

	private final List<SerializableField> fields = Lists.newArrayList();

	private final Set<Field> addedFields = Sets.newHashSet();

	private int nullableCount = 0;

	public ClassSerializerBuilder(Class<? extends T> ownerClass) {
		this.ownerClass = ownerClass;
	}

	public void appendField(Field field) {
		Preconditions.checkArgument(field.getDeclaringClass().isAssignableFrom(ownerClass), "%s does not belong to %s", field, ownerClass);

		final boolean newlyAdded = addedFields.add(field);
		Preconditions.checkState(newlyAdded, "%s already added", field);

		Serialize annotation = field.getAnnotation(Serialize.class);
		final boolean isNullable = !field.getType().isPrimitive() && (annotation != null && annotation.nullable());

		if (isNullable) nullableCount++;

		fields.add(new SerializableField(ownerClass, field, isNullable));
	}

	public IObjectSerializer<T> create() {
		return (nullableCount != 0)
				? new NullableSerializer<T>(fields, StreamUtils.bitsToBytes(nullableCount))
				: new NonNullableSerializer<T>(fields);
	}
}
