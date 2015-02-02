package openmods.network.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import openmods.serializable.cls.ClassSerializer;

public class SerializableNetworkEvent extends NetworkEvent {

	private static final ClassSerializer<SerializableNetworkEvent> SERIALIZER = new ClassSerializer<SerializableNetworkEvent>();

	@Override
	protected void readFromStream(DataInput input) throws IOException {
		SERIALIZER.readFromStream(this, input);
	}

	@Override
	protected void writeToStream(DataOutput output) throws IOException {
		SERIALIZER.writeToStream(this, output);
	}

}
