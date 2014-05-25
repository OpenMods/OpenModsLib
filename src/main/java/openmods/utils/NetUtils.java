package openmods.utils;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.lang.reflect.Field;
import java.util.Set;

import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.WorldServer;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class NetUtils {

	public static NetworkDispatcher getPlayerDispatcher(EntityPlayerMP player) {
		NetworkDispatcher dispatcher = player.playerNetServerHandler.netManager.channel().attr(NetworkDispatcher.FML_DISPATCHER).get();
		return dispatcher;
	}

	private static Field trackingPlayers;

	public static Set<EntityPlayerMP> getPlayersWatchingEntity(WorldServer server, int entityId) {
		EntityTracker tracker = server.getEntityTracker();

		if (trackingPlayers == null) trackingPlayers = ReflectionHelper.findField(EntityTracker.class, "trackedEntityIDs", "field_72794_c");

		IntHashMap trackers;
		try {
			trackers = (IntHashMap)trackingPlayers.get(tracker);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		EntityTrackerEntry entry = (EntityTrackerEntry)trackers.lookup(entityId);

		if (entry == null) return ImmutableSet.of();

		@SuppressWarnings({ "unchecked" })
		Set<EntityPlayerMP> trackingPlayers = entry.trackingPlayers;

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

	public static final ChannelFutureListener THROWING_LISTENER = new ChannelFutureListener() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (!future.isSuccess()) {
				Throwable cause = future.cause();
				Throwables.propagateIfInstanceOf(cause, Exception.class);
				Throwables.propagateIfInstanceOf(cause, Error.class);
				throw Throwables.propagate(cause);
			}
		}
	};

}
