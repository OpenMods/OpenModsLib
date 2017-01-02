package openmods.network.rpc;

import java.io.IOException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

public interface IRpcTarget {

	public Object getTarget();

	public void writeToStream(PacketBuffer output) throws IOException;

	public void readFromStreamStream(EntityPlayer player, PacketBuffer input) throws IOException;

	public void afterCall();
}
