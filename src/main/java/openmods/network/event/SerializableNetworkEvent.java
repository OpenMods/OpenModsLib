package openmods.network.event;

import java.io.IOException;

import net.minecraft.network.PacketBuffer;
import openmods.serializable.cls.ClassSerializersProvider;

public class SerializableNetworkEvent extends NetworkEvent {

	@Override
	protected void readFromStream(PacketBuffer input) throws IOException {
		ClassSerializersProvider.instance.readFromStream(this, input);
	}

	@Override
	protected void writeToStream(PacketBuffer output) throws IOException {
		ClassSerializersProvider.instance.writeToStream(this, output);
	}

}
