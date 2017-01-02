package openmods.tileentity;

import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.network.senders.IPacketSender;
import openmods.reflection.TypeUtils;
import openmods.sync.ISyncListener;
import openmods.sync.ISyncMapProvider;
import openmods.sync.ISyncableObject;
import openmods.sync.SyncMap;
import openmods.sync.SyncMapTile;
import openmods.sync.SyncObjectScanner;
import openmods.sync.drops.DropTagSerializer;

public abstract class SyncedTileEntity extends OpenTileEntity implements ISyncMapProvider {

	protected SyncMapTile<SyncedTileEntity> syncMap;

	private DropTagSerializer tagSerializer;

	public SyncedTileEntity() {
		syncMap = new SyncMapTile<SyncedTileEntity>(this);
		createSyncedFields();
		SyncObjectScanner.INSTANCE.registerAllFields(syncMap, this);

		syncMap.addSyncListener(new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				markUpdated();
			}
		});
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

	protected void markBlockForRenderUpdate(final BlockPos pos) {
		final int x = pos.getX();
		final int y = pos.getY();
		final int z = pos.getZ();
		worldObj.markBlockRangeForRenderUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
	}

	protected abstract void createSyncedFields();

	public void addSyncedObject(String name, ISyncableObject obj) {
		syncMap.put(name, obj);
	}

	public void sync() {
		syncMap.sync();
	}

	@Override
	public SyncMap<SyncedTileEntity> getSyncMap() {
		return syncMap;
	}

	// TODO verify if initial NBT send is enough

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		syncMap.writeToNBT(tag);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		syncMap.readFromNBT(tag);
	}

	public <T> T createRpcProxy(ISyncableObject object, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(object, mainIntf, extraIntf);
		IRpcTarget target = new SyncRpcTarget.SyncTileEntityRpcTarget(this, object);
		final IPacketSender sender = RpcCallDispatcher.INSTANCE.senders.client;
		return RpcCallDispatcher.INSTANCE.createProxy(target, sender, mainIntf, extraIntf);
	}
}
