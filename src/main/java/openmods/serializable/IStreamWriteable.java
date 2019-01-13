package openmods.serializable;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface IStreamWriteable {

	void writeToStream(PacketBuffer output) throws IOException;

}
