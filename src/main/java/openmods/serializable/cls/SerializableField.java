package openmods.serializable.cls;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;

import openmods.reflection.FieldAccess;
import openmods.serializable.IObjectSerializer;
import openmods.serializable.SerializerRegistry;
import openmods.utils.io.IStreamSerializer;

import com.google.common.base.Preconditions;

public class SerializableField<T> extends FieldAccess<Object> implements IObjectSerializer<T> {

	private final IStreamSerializer<Object> serializer;

	public SerializableField(Field field, IStreamSerializer<Object> serializer) {
		super(field);
		Preconditions.checkNotNull(serializer, "Empty serializer");
		this.serializer = serializer;
	}

	public SerializableField(Field field) {
		super(field);

		final Class<?> fieldType = field.getType();
		this.serializer = SerializerRegistry.instance.findSerializer(fieldType);
		Preconditions.checkNotNull(serializer, "Invalid field %s type", field);
	}

	@Override
	public void writeToStream(T target, DataOutput output) throws IOException {
		Object value = get(target);
		serializer.writeToStream(value, output);
	}

	@Override
	public void readFromStream(T target, DataInput input) throws IOException {
		Object value = serializer.readFromStream(input);
		set(target, value);
	}
}