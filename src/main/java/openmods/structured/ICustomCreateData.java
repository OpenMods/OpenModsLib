package openmods.structured;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface ICustomCreateData {

	public void readCustomDataFromStream(PacketBuffer input) throws IOException;

	public void writeCustomDataFromStream(PacketBuffer output) throws IOException;
}
