package openmods.serializable.providers;

import openmods.serializable.IObjectSerializer;
import openmods.serializable.ISerializerProvider;
import openmods.serializable.SerializerAdapters;
import openmods.serializable.cls.ClassSerializersProvider;
import openmods.serializable.cls.SerializableClass;
import openmods.utils.io.IStreamSerializer;

public class ClassSerializerProvider implements ISerializerProvider {

	@Override
	public IStreamSerializer<?> getSerializer(Class<?> cls) {
		if (cls.isAnnotationPresent(SerializableClass.class)) {
			IObjectSerializer<Object> objectSerializer = ClassSerializersProvider.instance.getSerializer(cls);
			return SerializerAdapters.createFromObjectSerializer(cls, objectSerializer);
		}
		return null;
	}

}