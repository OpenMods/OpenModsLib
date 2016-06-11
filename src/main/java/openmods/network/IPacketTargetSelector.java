package openmods.network;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;
import java.util.Collection;

public interface IPacketTargetSelector {
	public boolean isAllowedOnSide(Side side);

	public void listDispatchers(Object arg, Collection<NetworkDispatcher> result);
}
