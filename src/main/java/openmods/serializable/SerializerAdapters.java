package openmods.serializable;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;
import openmods.reflection.ConstructorAccess;
import openmods.utils.io.IStreamReader;
import openmods.utils.io.IStreamSerializer;

public class SerializerAdapters {

	public static <T extends IStreamWriteable & IStreamReadable> IStreamSerializer<T> createFromFactory(final IInstanceFactory<T> factory) {
		return new IStreamSerializer<T>() {
			@Override
			public T readFromStream(PacketBuffer input) throws IOException {
				T instance = factory.create();
				instance.readFromStream(input);
				return instance;
			}

			@Override
			public void writeToStream(T o, PacketBuffer output) throws IOException {
				o.writeToStream(output);
			}
		};
	}

	public static <T extends IStreamWriteable> IStreamSerializer<T> createFromReader(final IStreamReader<T> reader) {
		return new IStreamSerializer<T>() {
			@Override
			public T readFromStream(PacketBuffer input) throws IOException {
				return reader.readFromStream(input);
			}

			@Override
			public void writeToStream(T o, PacketBuffer output) throws IOException {
				o.writeToStream(output);
			}
		};
	}

	public static <T> IStreamSerializer<T> createFromObjectSerializer(final IInstanceFactory<T> factory, final IObjectSerializer<T> serializer) {
		return new IStreamSerializer<T>() {
			@Override
			public T readFromStream(PacketBuffer input) throws IOException {
				T object = factory.create();
				serializer.readFromStream(object, input);
				return object;
			}

			@Override
			public void writeToStream(T o, PacketBuffer output) throws IOException {
				serializer.writeToStream(o, output);
			}
		};
	}

	public static <T> IStreamSerializer<T> createFromObjectSerializer(Class<? extends T> cls, IObjectSerializer<T> serializer) {
		IInstanceFactory<T> factory = ConstructorAccess.create(cls);
		return createFromObjectSerializer(factory, serializer);
	}
}
