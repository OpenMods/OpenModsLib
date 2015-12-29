package openmods.network;

import java.util.Collection;

import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;

public interface IPacketTargetSelector {
	public boolean isAllowedOnSide(Side side);

	public void listDispatchers(Object arg, Collection<NetworkDispatcher> result);
}
