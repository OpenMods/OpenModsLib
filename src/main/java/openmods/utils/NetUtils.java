package openmods.utils;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import openmods.Log;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class NetUtils {

	public static NetworkDispatcher getPlayerDispatcher(EntityPlayerMP player) {
		NetworkDispatcher dispatcher = player.playerNetServerHandler.netManager.channel().attr(NetworkDispatcher.FML_DISPATCHER).get();
		return dispatcher;
	}

	public static Set<EntityPlayerMP> getPlayersWatchingEntity(WorldServer server, Entity entity) {
		final EntityTracker tracker = server.getEntityTracker();

		@SuppressWarnings("unchecked")
		final Set<? extends EntityPlayerMP> trackingPlayers = (Set<? extends EntityPlayerMP>)tracker.getTrackingPlayers(entity);
		return ImmutableSet.copyOf(trackingPlayers);
	}

	public static Set<EntityPlayerMP> getPlayersWatchingChunk(WorldServer world, int chunkX, int chunkZ) {
		PlayerManager manager = world.getPlayerManager();

		Set<EntityPlayerMP> playerList = Sets.newHashSet();
		for (Object o : world.playerEntities) {
			EntityPlayerMP player = (EntityPlayerMP)o;
			if (manager.isPlayerWatchingChunk(player, chunkX, chunkZ)) playerList.add(player);
		}
		return playerList;
	}

	public static Set<EntityPlayerMP> getPlayersWatchingBlock(WorldServer world, int blockX, int blockZ) {
		return getPlayersWatchingChunk(world, blockX >> 4, blockZ >> 4);
	}

	public static final ChannelFutureListener LOGGING_LISTENER = new ChannelFutureListener() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (!future.isSuccess()) {
				Throwable cause = future.cause();
				Log.severe(cause, "Crash in pipeline handler");
			}
		}
	};

}
