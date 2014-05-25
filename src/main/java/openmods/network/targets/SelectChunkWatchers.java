package openmods.network.targets;

import java.util.Collection;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import openmods.network.DimCoord;
import openmods.network.IPacketTargetSelector;
import openmods.utils.NetUtils;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.Side;

public class SelectChunkWatchers implements IPacketTargetSelector {

	@Override
	public boolean isAllowedOnSide(Side side) {
		return side == Side.SERVER;
	}

	@Override
	public void listDispatchers(Object arg, Collection<NetworkDispatcher> result) {
		Preconditions.checkArgument(arg instanceof DimCoord, "Argument must be DimCoord");
		DimCoord coord = (DimCoord)arg;

		WorldServer server = MinecraftServer.getServer().worldServers[coord.dimension];

		Set<EntityPlayerMP> players = NetUtils.getPlayersWatchingBlock(server, coord.x, coord.z);

		for (EntityPlayerMP player : players) {
			NetworkDispatcher dispatcher = NetUtils.getPlayerDispatcher(player);
			result.add(dispatcher);
		}
	}

}
