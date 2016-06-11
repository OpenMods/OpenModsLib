package openmods.sync;

import java.util.Set;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import openmods.utils.NetUtils;

public class SyncMapTile<H extends TileEntity & ISyncMapProvider> extends SyncMap<H> {

	public SyncMapTile(H handler) {
		super(handler);
	}

	@Override
	protected SyncMap.HandlerType getHandlerType() {
		return HandlerType.TILE_ENTITY;
	}

	@Override
	protected Set<EntityPlayerMP> getPlayersWatching() {
		return NetUtils.getPlayersWatchingBlock((WorldServer)handler.getWorldObj(), handler.xCoord, handler.zCoord);
	}

	@Override
	protected World getWorld() {
		return handler.getWorldObj();
	}

	@Override
	protected boolean isInvalid() {
		return handler.isInvalid();
	}
}
