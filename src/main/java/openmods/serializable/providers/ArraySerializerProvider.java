package openmods.serializable.providers;

import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Array;
import net.minecraft.network.PacketBuffer;
import openmods.serializable.ISerializerProvider;
import openmods.serializable.SerializerRegistry;
import openmods.utils.io.IStreamSerializer;

public class ArraySerializerProvider implements ISerializerProvider {

	@Override
	public IStreamSerializer<?> getSerializer(Class<?> cls) {
		if (cls.isArray()) {
			final TypeToken<?> componentCls = TypeToken.of(cls).getComponentType();
			return componentCls.isPrimitive()
					? createPrimitiveSerializer(componentCls)
					: createNullableSerializer(componentCls);
		}

		return null;
	}

	private static IStreamSerializer<?> createPrimitiveSerializer(final TypeToken<?> componentType) {
		final IStreamSerializer<Object> componentSerializer = SerializerRegistry.instance.findSerializer(componentType.getType());
		final Class<?> componentCls = componentType.getRawType();
		return new IStreamSerializer<Object>() {
			@Override
			public Object readFromStream(PacketBuffer input) throws IOException {
				final int length = input.readVarIntFromBuffer();
				Object result = Array.newInstance(componentCls, length);

				for (int i = 0; i < length; i++) {
					final Object value = componentSerializer.readFromStream(input);
					Array.set(result, i, value);
				}

				return result;
			}

			@Override
			public void writeToStream(Object o, PacketBuffer output) throws IOException {
				final int length = Array.getLength(o);
				output.writeVarIntToBuffer(length);

				for (int i = 0; i < length; i++) {
					Object value = Array.get(o, i);
					componentSerializer.writeToStream(value, output);
				}
			}
		};
	}

	private static IStreamSerializer<?> createNullableSerializer(final TypeToken<?> componentType) {
		return new NullableCollectionSerializer<Object>(componentType) {

			@Override
			protected Object createCollection(TypeToken<?> componentCls, int length) {
				return Array.newInstance(componentCls.getRawType(), length);
			}

			@Override
			protected int getLength(Object collection) {
				return Array.getLength(collection);
			}

			@Override
			protected Object getElement(Object collection, int index) {
				return Array.get(collection, index);
			}

			@Override
			protected void setElement(Object collection, int index, Object value) {
				Array.set(collection, index, value);
			}

		};
	}
}
