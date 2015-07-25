package openmods.structured;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;

import openmods.reflection.InstanceFieldAccess;
import openmods.reflection.TypeUtils;
import openmods.serializable.SerializerRegistry;
import openmods.utils.io.IStreamSerializer;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;

public class ElementField extends InstanceFieldAccess<Object> implements IStructureElement {

	private int elementId;
	public final IStreamSerializer<Object> serializer;

	public ElementField(Object parent, Field field) {
		super(parent, field);

		final TypeToken<?> fieldType = TypeUtils.resolveFieldType(parent.getClass(), field);
		this.serializer = SerializerRegistry.instance.findSerializer(fieldType.getType());
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

	@Override
	public int getId() {
		return elementId;
	}

	@Override
	public void setId(int id) {
		this.elementId = id;
	}
}