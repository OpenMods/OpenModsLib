package openmods.serializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import openmods.reflection.ConstructorAccess;
import openmods.reflection.TypeUtils;
import openmods.serializable.providers.ArraySerializerProvider;
import openmods.serializable.providers.EnumSerializerProvider;
import openmods.utils.io.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SerializerRegistry {

	public static final SerializerRegistry instance = new SerializerRegistry();

	private final Map<Class<?>, IStreamSerializer<?>> serializers = Maps.newHashMap(TypeRW.STREAM_SERIALIZERS);

	private final List<ISerializerProvider> providers = Lists.newArrayList();

	{
		providers.add(new EnumSerializerProvider());
		providers.add(new ArraySerializerProvider());
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<? extends T> resolve(Class<?> intf, Class<?> concrete) {
		final Class<?> rawType = TypeUtils.getTypeParameter(intf, concrete).getRawType();
		return (Class<? extends T>)rawType;
	}

	public <T> void register(Class<? extends T> target, IStreamSerializer<T> serializer) {
		Preconditions.checkArgument(target != Object.class, "Can't register serializer for Object");
		final IStreamSerializer<?> prev = serializers.put(target, serializer);
		Preconditions.checkState(prev == null, "Duplicate serializer for %s", target);
	}

	public <T> void register(IStreamSerializer<T> serializer) {
		Class<? extends T> cls = resolve(IStreamSerializer.class, serializer.getClass());
		register(cls, serializer);
	}

	public <T extends IStreamWriteable & IStreamReadable> void registerSerializable(Class<? extends T> cls, IInstanceFactory<T> factory) {
		register(cls, SerializerAdapters.createFromFactory(factory));
	}

	public <T extends IStreamWriteable & IStreamReadable> void registerSerializable(IInstanceFactory<T> factory) {
		Class<? extends T> cls = resolve(IInstanceFactory.class, factory.getClass());
		registerSerializable(cls, factory);
	}

	public <T extends IStreamWriteable & IStreamReadable> void registerSerializable(Class<T> cls) {
		IInstanceFactory<T> factory = ConstructorAccess.create(cls);
		registerSerializable(cls, factory);
	}

	public <T extends IStreamWriteable> void registerWriteable(Class<? extends T> cls, IStreamReader<T> reader) {
		register(cls, SerializerAdapters.createFromReader(reader));
	}

	public <T extends IStreamWriteable> void registerWriteable(IStreamReader<T> reader) {
		Class<? extends T> cls = resolve(IStreamReader.class, reader.getClass());
		registerWriteable(cls, reader);
	}

	public void registerProvider(ISerializerProvider provider) {
		Preconditions.checkNotNull(provider);
		providers.add(provider);
	}

	@SuppressWarnings("unchecked")
	public <T> IStreamSerializer<T> findSerializer(Class<? extends T> cls) {
		IStreamSerializer<?> serializer = serializers.get(cls);

		if (serializer == null) {
			for (ISerializerProvider provider : providers) {
				serializer = provider.getSerializer(cls);
				if (serializer != null) {
					serializers.put(cls, serializer);
					break;
				}
			}
		}

		return (IStreamSerializer<T>)serializer;
	}

	public <T> T createFromStream(DataInput input, Class<? extends T> cls) throws IOException {
		IStreamReader<T> reader = findSerializer(cls);
		Preconditions.checkNotNull(reader, "Can't find reader for class %s", cls);
		return reader.readFromStream(input);
	}

	@SuppressWarnings("unchecked")
	public <T> void writeToStream(DataOutput output, T target) throws IOException {
		Preconditions.checkNotNull(target);

		final Class<? extends T> cls = (Class<? extends T>)target.getClass();
		IStreamWriter<T> writer = findSerializer(cls);
		Preconditions.checkNotNull(writer, "Can't find writer for class %s", cls);
		writer.writeToStream(target, output);
	}
}
