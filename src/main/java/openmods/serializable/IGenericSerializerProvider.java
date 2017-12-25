package openmods.serializable;

import java.lang.reflect.Type;
import openmods.utils.io.IStreamSerializer;

@FunctionalInterface
public interface IGenericSerializerProvider {
	public IStreamSerializer<?> getSerializer(Type type);
}
