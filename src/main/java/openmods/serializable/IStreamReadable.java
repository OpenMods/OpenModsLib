package openmods.serializable;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface IStreamReadable {

	public void readFromStream(PacketBuffer input) throws IOException;

}
