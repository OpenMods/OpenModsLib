package openmods.serializable.cls;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import openmods.serializable.IObjectSerializer;

import com.google.common.collect.ImmutableList;

public class ComposedSerializer<T> implements IObjectSerializer<T> {

	private final List<IObjectSerializer<T>> serializers;

	public ComposedSerializer(List<IObjectSerializer<T>> serializers) {
		this.serializers = ImmutableList.copyOf(serializers);
	}

	@Override
	public void writeToStream(T target, DataOutput output) throws IOException {
		for (IObjectSerializer<T> field : serializers)
			field.writeToStream(target, output);
	}

	@Override
	public void readFromStream(T target, DataInput input) throws IOException {
		for (IObjectSerializer<T> field : serializers)
			field.readFromStream(target, input);
	}
}