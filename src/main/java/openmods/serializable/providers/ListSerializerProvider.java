package openmods.serializable.providers;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import openmods.reflection.TypeUtils;
import openmods.serializable.IGenericSerializerProvider;
import openmods.utils.io.IStreamSerializer;

import com.google.common.reflect.TypeToken;

public class ListSerializerProvider implements IGenericSerializerProvider {

	@Override
	public IStreamSerializer<?> getSerializer(Type type) {
		TypeToken<?> typeToken = TypeToken.of(type);

		if (TypeUtils.LIST_TOKEN.isAssignableFrom(typeToken)) {
			final TypeToken<?> componentType = typeToken.resolveType(TypeUtils.LIST_VALUE_PARAM);

			return new NullableCollectionSerializer<List<Object>>(componentType) {

				@Override
				protected List<Object> createCollection(TypeToken<?> componentCls, int length) {
					return Arrays.asList(new Object[length]);
				}

				@Override
				protected int getLength(List<Object> collection) {
					return collection.size();
				}

				@Override
				protected Object getElement(List<Object> collection, int index) {
					return collection.get(index);
				}

				@Override
				protected void setElement(List<Object> collection, int index, Object value) {
					collection.set(index, value);
				}
			};
		}

		return null;
	}

}
