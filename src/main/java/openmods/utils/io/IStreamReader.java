package openmods.utils.io;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface IStreamReader<T> {
	T readFromStream(PacketBuffer input) throws IOException;
}