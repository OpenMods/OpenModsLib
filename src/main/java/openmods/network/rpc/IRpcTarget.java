package openmods.network.rpc;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;

public interface IRpcTarget {

	Object getTarget();

	void writeToStream(PacketBuffer output) throws IOException;

	void readFromStreamStream(LogicalSide side, PacketBuffer input) throws IOException;

	void afterCall();
}
