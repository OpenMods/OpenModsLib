package openmods.serializable;

import openmods.utils.io.IStreamSerializer;

public interface ISerializerProvider {
	IStreamSerializer<?> getSerializer(Class<?> cls);
}
