package openmods.serializable.cls;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;

import openmods.reflection.FieldAccess;
import openmods.serializable.IObjectSerializer;
import openmods.utils.io.IStreamSerializer;
import openmods.utils.io.TypeRW;

import com.google.common.base.Preconditions;

public class SerializableField<T> extends FieldAccess<Object> implements IObjectSerializer<T> {

	private final IStreamSerializer<Object> serializer;

	public SerializableField(Field field, IStreamSerializer<Object> serializer) {
		super(field);
		Preconditions.checkNotNull(serializer, "Empty serializer");
		this.serializer = serializer;
	}

	@SuppressWarnings("unchecked")
	public SerializableField(Field field) {
		super(field);

		Class<?> fieldType = field.getType();
		this.serializer = (IStreamSerializer<Object>)TypeRW.STREAM_SERIALIZERS.get(fieldType);
		Preconditions.checkNotNull(serializer, "Invalid field type");
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