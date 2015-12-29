package openmods.sync;

import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import openmods.utils.NetUtils;

public class SyncMapEntity<H extends Entity & ISyncMapProvider> extends SyncMap<H> {

	public SyncMapEntity(H handler) {
		super(handler);
	}

	@Override
	protected SyncMap.HandlerType getHandlerType() {
		return HandlerType.ENTITY;
	}

	@Override
	protected Set<EntityPlayerMP> getPlayersWatching() {
		return NetUtils.getPlayersWatchingEntity((WorldServer)handler.worldObj, handler);
	}

	@Override
	protected World getWorld() {
		return handler.worldObj;
	}

	@Override
	protected boolean isInvalid() {
		return handler.isDead;
	}
}