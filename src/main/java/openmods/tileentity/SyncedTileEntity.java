package openmods.tileentity;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.Log;
import openmods.network.rpc.IRpcTarget;
import openmods.network.rpc.RpcCallDispatcher;
import openmods.network.rpc.targets.SyncRpcTarget;
import openmods.sync.*;
import openmods.utils.TypeUtils;

public abstract class SyncedTileEntity extends OpenTileEntity implements ISyncMapProvider {

	protected SyncMapTile<SyncedTileEntity> syncMap;

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

	protected ISyncListener createRenderUpdateListener() {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
		};
	}

	protected ISyncListener createRenderUpdateListener(final ISyncableObject target) {
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				if (changes.contains(target)) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
		};
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

	@Override
	public Packet getDescriptionPacket() {
		try {
			ByteBuf payload = syncMap.createPayload(true);
			return SyncChannelHolder.createPacket(payload);
		} catch (IOException e) {
			Log.severe(e, "Error during description packet creation");
			return null;
		}
	}

	public ForgeDirection getSecondaryRotation() {
		ISyncableObject rot = syncMap.get("_rotation2");
		if (rot != null) { return ((SyncableDirection)rot).getValue(); }
		return null;
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		syncMap.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		syncMap.readFromNBT(tag);
	}

	public <T> T createRpcProxy(ISyncableObject object, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		TypeUtils.isInstance(object, mainIntf, extraIntf);
		IRpcTarget target = new SyncRpcTarget.SyncTileEntityRpcTarget(this, object);
		return RpcCallDispatcher.INSTANCE.createProxy(target, mainIntf, extraIntf);
	}

}
