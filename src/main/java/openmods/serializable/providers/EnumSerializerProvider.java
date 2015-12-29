package openmods.serializable.providers;

import net.minecraft.network.PacketBuffer;
import openmods.serializable.ISerializerProvider;
import openmods.utils.io.IStreamSerializer;

public class EnumSerializerProvider implements ISerializerProvider {

	@Override
	public IStreamSerializer<?> getSerializer(final Class<?> cls) {
		// for simplicity I'm allowing multiple entries for enum with subclasses
		return Enum.class.isAssignableFrom(cls)? createSerializer(cls) : null;
	}

	private static IStreamSerializer<?> createSerializer(final Class<?> cls) {
		final Class<?> superCls = cls.getSuperclass();
		final Object[] values = superCls == Enum.class? cls.getEnumConstants() : superCls.getEnumConstants();

		return new IStreamSerializer<Object>() {
			@Override
			public Object readFromStream(PacketBuffer input) {
				final int ord = input.readVarIntFromBuffer();

				try {
					return values[ord];
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new ArrayIndexOutOfBoundsException(String.format("Failed to get enum with ordinal %d from class %s", ord, cls));
				}
			}

			@Override
			public void writeToStream(Object o, PacketBuffer output) {
				final int ord = ((Enum<?>)o).ordinal();
				output.writeVarIntToBuffer(ord);
			}
		};
	}

}
