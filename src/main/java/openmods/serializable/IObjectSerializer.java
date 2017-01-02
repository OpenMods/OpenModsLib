package openmods.serializable;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface IObjectSerializer<T> {
	public void readFromStream(T object, PacketBuffer input) throws IOException;

	public void writeToStream(T object, PacketBuffer output) throws IOException;
}
