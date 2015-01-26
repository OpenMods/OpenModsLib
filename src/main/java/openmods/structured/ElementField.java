package openmods.structured;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;

import openmods.reflection.InstanceFieldAccess;
import openmods.utils.io.IStreamSerializer;
import openmods.utils.io.TypeRW;

import com.google.common.base.Preconditions;

public class ElementField extends InstanceFieldAccess<Object> implements IStructureElement {

	public int elementId;
	public final IStreamSerializer<Object> serializer;

	@SuppressWarnings("unchecked")
	public ElementField(Object parent, Field field) {
		super(parent, field);

		Class<?> fieldType = field.getType();
		serializer = (IStreamSerializer<Object>)TypeRW.TYPES.get(fieldType);
		Preconditions.checkNotNull(serializer, "Invalid field type");
	}

	@Override
	public void writeToStream(DataOutput output) throws IOException {
		Object value = get();
		serializer.writeToStream(value, output);
	}

	@Override
	public void readFromStream(DataInput input) throws IOException {
		Object value = serializer.readFromStream(input);
		set(value);
	}
}