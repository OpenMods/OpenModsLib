package openmods.network;

import java.util.Collection;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;

public interface IPacketTargetSelector {
	public boolean isAllowedOnSide(Side side);

	public void listDispatchers(Object arg, Collection<NetworkDispatcher> result);
}
