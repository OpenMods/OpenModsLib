package openmods.serializable.cls;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import openmods.serializable.IObjectSerializer;

import com.google.common.collect.Lists;

public class FieldsSerializer<T> implements IObjectSerializer<T> {

	private final List<SerializableField<T>> fields;

	public FieldsSerializer(List<SerializableField<T>> fields) {
		this.fields = fields;
	}

	@Override
	public void writeToStream(T target, DataOutput output) throws IOException {
		for (SerializableField<T> field : fields)
			field.writeToStream(target, output);
	}

	@Override
	public void readFromStream(T target, DataInput input) throws IOException {
		for (SerializableField<T> field : fields)
			field.readFromStream(target, input);
	}

	public static <T> FieldsSerializer<T> createFromFields(Iterable<Field> fields) {
		List<SerializableField<T>> result = Lists.newArrayList();

		for (Field f : fields)
			result.add(new SerializableField<T>(f));

		return new FieldsSerializer<T>(result);
	}
}