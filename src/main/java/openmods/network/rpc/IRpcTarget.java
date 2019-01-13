package openmods.network.rpc;

import java.io.IOException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;

public interface IRpcTarget {

	Object getTarget();

	void writeToStream(PacketBuffer output) throws IOException;

	void readFromStreamStream(Side side, EntityPlayer player, PacketBuffer input) throws IOException;

	void afterCall();
}
