package openmods.network.targets;

import java.util.Collection;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;
import openmods.Log;
import openmods.network.IPacketTargetSelector;
import openmods.utils.NetUtils;

public class SelectMultiplePlayers implements IPacketTargetSelector<Collection<ServerPlayerEntity>> {

	public static final IPacketTargetSelector<Collection<ServerPlayerEntity>> INSTANCE = new SelectMultiplePlayers();

	@Override
	public boolean isAllowedOnSide(Side side) {
		return side == Side.SERVER;
	}

	@Override
	public void listDispatchers(Collection<ServerPlayerEntity> players, Collection<NetworkDispatcher> result) {
		for (ServerPlayerEntity player : players) {
			NetworkDispatcher dispatcher = NetUtils.getPlayerDispatcher(player);
			if (dispatcher != null) result.add(dispatcher);
			else Log.info("Trying to send message to disconnected player %s", player);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<ServerPlayerEntity> castArg(Object arg) {
		return (Collection<ServerPlayerEntity>)arg;
	}

}
