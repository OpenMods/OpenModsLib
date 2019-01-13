package openmods.serializable;

import java.lang.reflect.Type;
import openmods.utils.io.IStreamSerializer;

@FunctionalInterface
public interface IGenericSerializerProvider {
	IStreamSerializer<?> getSerializer(Type type);
}
