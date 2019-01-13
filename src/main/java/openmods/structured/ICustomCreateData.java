package openmods.structured;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface ICustomCreateData {

	void readCustomDataFromStream(PacketBuffer input);

	void writeCustomDataFromStream(PacketBuffer output);
}
