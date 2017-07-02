package openmods.network.rpc;

import java.io.IOException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;

public interface IRpcTarget {

	public Object getTarget();

	public void writeToStream(PacketBuffer output) throws IOException;

	public void readFromStreamStream(Side side, EntityPlayer player, PacketBuffer input) throws IOException;

	public void afterCall();
}
