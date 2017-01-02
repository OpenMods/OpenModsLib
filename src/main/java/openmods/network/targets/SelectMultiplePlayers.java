package openmods.network.targets;

import java.util.Collection;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;
import openmods.Log;
import openmods.network.IPacketTargetSelector;
import openmods.utils.NetUtils;

public class SelectMultiplePlayers implements IPacketTargetSelector<Collection<EntityPlayerMP>> {

	public static final IPacketTargetSelector<Collection<EntityPlayerMP>> INSTANCE = new SelectMultiplePlayers();

	@Override
	public boolean isAllowedOnSide(Side side) {
		return side == Side.SERVER;
	}

	@Override
	public void listDispatchers(Collection<EntityPlayerMP> players, Collection<NetworkDispatcher> result) {
		for (EntityPlayerMP player : players) {
			NetworkDispatcher dispatcher = NetUtils.getPlayerDispatcher(player);
			if (dispatcher != null) result.add(dispatcher);
			else Log.info("Trying to send message to disconnected player %s", player);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<EntityPlayerMP> castArg(Object arg) {
		return (Collection<EntityPlayerMP>)arg;
	}

}
