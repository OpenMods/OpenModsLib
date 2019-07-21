package openmods.sync;

import java.util.Set;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.ServerWorld;
import openmods.tileentity.SyncedTileEntity;
import openmods.utils.NetUtils;

public class SyncMapTile extends SyncMapServer {

	private final SyncedTileEntity owner;

	public SyncMapTile(SyncedTileEntity owner, UpdateStrategy strategy) {
		super(strategy);
		this.owner = owner;
	}

	public static final int OWNER_TYPE = 1;

	public static ISyncMapProvider findOwner(World world, PacketBuffer input) {
		final BlockPos pos = input.readBlockPos();
		if (world != null) {
			if (world.isBlockLoaded(pos)) {
				final TileEntity tile = world.getTileEntity(pos);
				if (tile instanceof ISyncMapProvider) return (ISyncMapProvider)tile;
			}
		}

		return null;
	}

	@Override
	protected int getOwnerType() {
		return OWNER_TYPE;
	}

	@Override
	protected void writeOwnerData(PacketBuffer output) {
		output.writeBlockPos(owner.getPos());
	}

	@Override
	protected Set<ServerPlayerEntity> getPlayersWatching() {
		final BlockPos pos = owner.getPos();
		return NetUtils.getPlayersWatchingBlock((ServerWorld)owner.getWorld(), pos.getX(), pos.getZ());
	}

	@Override
	protected boolean isInvalid() {
		return owner.isInvalid();
	}
}
