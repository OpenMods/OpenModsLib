package openmods.utils.io;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;

public interface IStreamWriter<T> {
	public void writeToStream(T o, PacketBuffer output) throws IOException;
}