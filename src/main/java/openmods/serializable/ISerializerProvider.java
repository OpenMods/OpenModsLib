package openmods.serializable;

import openmods.utils.io.IStreamSerializer;

public interface ISerializerProvider {
	public IStreamSerializer<?> getSerializer(Class<?> cls);
}
