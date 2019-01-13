package openmods.serializable;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface IStreamReadable {

	void readFromStream(PacketBuffer input) throws IOException;

}
