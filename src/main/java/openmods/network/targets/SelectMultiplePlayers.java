package openmods.network.targets;

import java.util.Collection;

import net.minecraft.entity.player.EntityPlayerMP;
import openmods.Log;
import openmods.network.IPacketTargetSelector;
import openmods.utils.NetUtils;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;

public class SelectMultiplePlayers implements IPacketTargetSelector {

	public static final IPacketTargetSelector INSTANCE = new SelectMultiplePlayers();

	@Override
	public boolean isAllowedOnSide(Side side) {
		return side == Side.SERVER;
	}

	@Override
	public void listDispatchers(Object arg, Collection<NetworkDispatcher> result) {
		try {
			@SuppressWarnings("unchecked")
			Collection<EntityPlayerMP> players = (Collection<EntityPlayerMP>)arg;
			for (EntityPlayerMP player : players) {
				NetworkDispatcher dispatcher = NetUtils.getPlayerDispatcher(player);
				if (dispatcher != null) result.add(dispatcher);
				else Log.info("Trying to send message to disconnected player %s", player);
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Argument must be collection of EntityPlayerMP");
		}
	}

}
