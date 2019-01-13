package openmods.network;

import java.util.Collection;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;

public interface IPacketTargetSelector<T> {
	boolean isAllowedOnSide(Side side);

	T castArg(Object arg);

	void listDispatchers(T arg, Collection<NetworkDispatcher> result);
}
