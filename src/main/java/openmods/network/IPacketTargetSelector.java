package openmods.network;

import java.util.Collection;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;

public interface IPacketTargetSelector<T> {
	public boolean isAllowedOnSide(Side side);

	public T castArg(Object arg);

	public void listDispatchers(T arg, Collection<NetworkDispatcher> result);
}
