package openmods.network.rpc;

import java.io.IOException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;

public interface IRpcTarget {

	Object getTarget();

	void writeToStream(PacketBuffer output) throws IOException;

	void readFromStreamStream(Side side, PlayerEntity player, PacketBuffer input) throws IOException;

	void afterCall();
}
