package openmods.network.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import openmods.serializable.cls.ClassSerializer;

public class SerializableNetworkEvent extends NetworkEvent {

	@Override
	protected void readFromStream(DataInput input) throws IOException {
		ClassSerializer.instance.readFromStream(this, input);
	}

	@Override
	protected void writeToStream(DataOutput output) throws IOException {
		ClassSerializer.instance.writeToStream(this, output);
	}

}
