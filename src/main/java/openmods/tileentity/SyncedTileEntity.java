package openmods.tileentity;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.network.senders.IPacketSender;
import openmods.reflection.TypeUtils;
import openmods.sync.ISyncListener;
import openmods.sync.ISyncMapProvider;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncMap;
import openmods.sync.SyncMapClient;
import openmods.sync.SyncMapTile;
import openmods.sync.SyncObjectScanner;
import openmods.sync.drops.DropTagSerializer;

public abstract class SyncedTileEntity extends OpenTileEntity implements ISyncMapProvider {

	private static final String TAG_SYNC_INIT = "SyncInit";

	private SyncMap syncMap;

	private DropTagSerializer tagSerializer;

	public SyncedTileEntity() {
		createSyncedFields();
	}

	private void createSyncMap(World world) {
		final SyncMap syncMap = world.isRemote? new SyncMapClient() : new SyncMapTile(this);

		SyncObjectScanner.INSTANCE.registerAllFields(syncMap, this);

		syncMap.addSyncListener(new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				markUpdated();
			}
		});

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
	protected void setWorldCreate(World worldIn) {
		createSyncMap(worldIn);
	}

	protected DropTagSerializer getDropSerializer() {
		if (tagSerializer == null) tagSerializer = new DropTagSerializer();
		return tagSerializer;
	}

	protected ISyncListener createRenderUpdateListener() {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				markBlockForRenderUpdate(getPos());
			}
		};
	}

	protected ISyncListener createRenderUpdateListener(final ISyncableObject target) {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				if (changes.contains(target)) markBlockForRenderUpdate(getPos());
			}
		};
	}

	protected ISyncListener createRenderUpdateListener(final Set<ISyncableObject> targets) {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				if (!Sets.intersection(changes, targets).isEmpty()) markBlockForRenderUpdate(getPos());
			}
		};
	}

	protected void markBlockForRenderUpdate(BlockPos pos) {
		world.markBlockRangeForRenderUpdate(pos, pos);
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
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		getSyncMap().write(tag);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		getSyncMap().read(tag);
	}

	private NBTTagCompound serializeInitializationData(NBTTagCompound tag) {
		final PacketBuffer tmp = new PacketBuffer(Unpooled.buffer());
		try {
			getSyncMap().writeInitializationData(tmp);
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
		byte[] data = new byte[tmp.readableBytes()];
		tmp.readBytes(data);
		tag.setByteArray(TAG_SYNC_INIT, data);

		return tag;
	}

	private void applyInitializationData(NBTTagCompound tag) {
		if (tag.hasKey(TAG_SYNC_INIT, Constants.NBT.TAG_BYTE_ARRAY)) {
			final byte[] syncInit = tag.getByteArray(TAG_SYNC_INIT);
			final PacketBuffer tmp = new PacketBuffer(Unpooled.buffer());
			tmp.writeBytes(syncInit);

			try {
				getSyncMap().readIntializationData(tmp);
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		}
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return serializeInitializationData(super.getUpdateTag());
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.readFromNBT(tag);
		applyInitializationData(tag);
	}

	@Override
	@Nullable
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 43, serializeInitializationData(new NBTTagCompound()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		applyInitializationData(pkt.getNbtCompound());
	}

	public <T> T createRpcProxy(ISyncableObject object, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(object, mainIntf, extraIntf);
		IRpcTarget target = new SyncRpcTarget.SyncTileEntityRpcTarget(this, object);
		final IPacketSender sender = RpcCallDispatcher.instance().senders.client;
		return RpcCallDispatcher.instance().createProxy(target, sender, mainIntf, extraIntf);
	}
}
