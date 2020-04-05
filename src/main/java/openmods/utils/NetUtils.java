package openmods.utils;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;

public class NetUtils {

	public static Set<ServerPlayerEntity> getPlayersWatchingEntity(ServerWorld server, Entity entity) {
		final ChunkManager.EntityTracker tracker = server.getChunkProvider().chunkManager.entities.get(entity.getEntityId());
		if (tracker == null) {
			return ImmutableSet.of();
		}
		return ImmutableSet.copyOf(tracker.trackingPlayers);
	}

	public static Set<ServerPlayerEntity> getPlayersWatchingChunk(ServerWorld world, int chunkX, int chunkZ) {
		final ChunkManager chunkManager = world.getChunkProvider().chunkManager;
		return chunkManager.getTrackingPlayers(new ChunkPos(chunkX, chunkZ), false).collect(ImmutableSet.toImmutableSet());
	}

	public static Set<ServerPlayerEntity> getPlayersWatchingBlock(ServerWorld world, int blockX, int blockZ) {
		return getPlayersWatchingChunk(world, blockX >> 4, blockZ >> 4);
	}

}
