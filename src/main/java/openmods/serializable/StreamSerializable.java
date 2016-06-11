package openmods.serializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import openmods.utils.io.IStreamReader;
import openmods.utils.io.IStreamSerializer;
import openmods.utils.io.IStreamWriter;

public class StreamSerializable<T> implements IStreamSerializable {

	public T value;

	private final IStreamWriter<T> streamWriter;

	private final IStreamReader<T> streamReader;

	public StreamSerializable(T value, IStreamWriter<T> streamWriter, IStreamReader<T> streamReader) {
		this.value = value;
		this.streamWriter = streamWriter;
		this.streamReader = streamReader;
	}

	public StreamSerializable(T value, IStreamSerializer<T> streamSerializer) {
		this.value = value;
		this.streamWriter = streamSerializer;
		this.streamReader = streamSerializer;
	}

	@Override
	public void readFromStream(DataInput input) throws IOException {
		value = streamReader.readFromStream(input);
	}

	@Override
	public void writeToStream(DataOutput output) throws IOException {
		streamWriter.writeToStream(value, output);
	}

}
