package openmods.tileentity;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.network.PacketDistributor;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.reflection.TypeUtils;
import openmods.sync.ISyncListener;
import openmods.sync.ISyncMapProvider;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncMap;
import openmods.sync.SyncMapClient;
import openmods.sync.SyncMapServer.UpdateStrategy;
import openmods.sync.SyncMapTile;
import openmods.sync.SyncObjectScanner;
import openmods.sync.drops.DropTagSerializer;

public abstract class SyncedTileEntity extends OpenTileEntity implements ISyncMapProvider {

	private static final String TAG_SYNC_INIT = "SyncInit";

	private SyncMap syncMap;

	private DropTagSerializer tagSerializer;

	public SyncedTileEntity(TileEntityType<?> type) {
		super(type);
		createSyncedFields();
	}

	private void createSyncMap(World world) {
		final SyncMap syncMap = world.isRemote? new SyncMapClient() : new SyncMapTile(this, UpdateStrategy.WITH_INITIAL_PACKET);

		SyncObjectScanner.INSTANCE.registerAllFields(syncMap, this);

		syncMap.addSyncListener(changes -> markUpdated());

		this.syncMap = syncMap;
		onSyncMapCreate(syncMap);
	}

	protected void onSyncMapCreate(SyncMap syncMap) {}

	@Override
	public void validate() {
		super.validate();
		createSyncMap(world);
	}

	@Override
	public void setWorld(World worldIn) {
		createSyncMap(worldIn);
	}

	protected DropTagSerializer getDropSerializer() {
		if (tagSerializer == null) tagSerializer = new DropTagSerializer();
		return tagSerializer;
	}

	protected ISyncListener createRenderUpdateListener() {
		return changes -> markBlockForRenderUpdate(getPos());
	}

	protected ISyncListener createRenderUpdateListener(final ISyncableObject target) {
		return changes -> {
			if (changes.contains(target)) markBlockForRenderUpdate(getPos());
		};
	}

	protected ISyncListener createRenderUpdateListener(final Set<ISyncableObject> targets) {
		return changes -> {
			if (!Sets.intersection(changes, targets).isEmpty()) markBlockForRenderUpdate(getPos());
		};
	}

	protected void markBlockForRenderUpdate(BlockPos pos) {
		world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), Constants.BlockFlags.DEFAULT);
	}

	protected abstract void createSyncedFields();

	public void sync() {
		getSyncMap().sendUpdates();
	}

	public boolean trySync() {
		return getSyncMap().trySendUpdates();
	}

	@Override
	public SyncMap getSyncMap() {
		Preconditions.checkState(syncMap != null, "Tile entity not initialized properly");
		return syncMap;
	}

	// TODO verify if initial NBT send is enough

	@Override
	public CompoundNBT write(CompoundNBT tag) {
		super.write(tag);
		getSyncMap().tryWrite(tag);
		return tag;
	}

	@Override
	public void read(CompoundNBT tag) {
		super.read(tag);
		getSyncMap().tryRead(tag);
	}

	private CompoundNBT serializeInitializationData(CompoundNBT tag) {
		final PacketBuffer tmp = new PacketBuffer(Unpooled.buffer());
		try {
			getSyncMap().writeInitializationData(tmp);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		byte[] data = new byte[tmp.readableBytes()];
		tmp.readBytes(data);
		tag.putByteArray(TAG_SYNC_INIT, data);

		return tag;
	}

	private void applyInitializationData(CompoundNBT tag) {
		if (tag.contains(TAG_SYNC_INIT, Constants.NBT.TAG_BYTE_ARRAY)) {
			final byte[] syncInit = tag.getByteArray(TAG_SYNC_INIT);
			final PacketBuffer tmp = new PacketBuffer(Unpooled.buffer());
			tmp.writeBytes(syncInit);

			try {
				getSyncMap().readIntializationData(tmp);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public CompoundNBT getUpdateTag() {
		return serializeInitializationData(super.getUpdateTag());
	}

	@Override
	public void handleUpdateTag(CompoundNBT tag) {
		super.read(tag);
		applyInitializationData(tag);
	}

	@Override
	@Nullable
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getPos(), 43, serializeInitializationData(new CompoundNBT()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		applyInitializationData(pkt.getNbtCompound());
	}

	public <T> T createRpcProxy(ISyncableObject object, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(object, mainIntf, extraIntf);
		IRpcTarget target = new SyncRpcTarget.SyncTileEntityRpcTarget(this, object);
		return RpcCallDispatcher.instance().createProxy(target, PacketDistributor.SERVER.noArg(), mainIntf, extraIntf);
	}
}
