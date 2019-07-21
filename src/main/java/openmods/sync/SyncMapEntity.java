package openmods.sync;

import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import openmods.Log;
import openmods.utils.NetUtils;

public class SyncMapEntity extends SyncMapServer {

	private final Entity owner;

	public SyncMapEntity(Entity owner, UpdateStrategy strategy) {
		super(strategy);
		this.owner = owner;
	}

	public static final int OWNER_TYPE = 0;

	public static ISyncMapProvider findOwner(World world, PacketBuffer input) {
		int entityId = input.readInt();
		Entity entity = world.getEntityByID(entityId);
		if (entity instanceof ISyncMapProvider)
			return (ISyncMapProvider)entity;

		Log.warn("Invalid handler info: can't find ISyncHandler entity id %d", entityId);
		return null;
	}

	@Override
	protected int getOwnerType() {
		return OWNER_TYPE;
	}

	@Override
	protected void writeOwnerData(PacketBuffer outputBuffer) {
		outputBuffer.writeInt(owner.getEntityId());
	}

	@Override
	protected Set<ServerPlayerEntity> getPlayersWatching() {
		return NetUtils.getPlayersWatchingEntity((ServerWorld)owner.world, owner);
	}

	@Override
	protected boolean isInvalid() {
		return owner.isDead;
	}
}