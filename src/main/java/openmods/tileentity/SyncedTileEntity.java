package openmods.tileentity;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.Log;
import openmods.sync.*;

public abstract class SyncedTileEntity extends OpenTileEntity implements ISyncHandler {

	protected SyncMapTile<SyncedTileEntity> syncMap;

	public SyncedTileEntity() {
		syncMap = new SyncMapTile<SyncedTileEntity>(this);
		createSyncedFields();
		syncMap.autoregister();
	}

	protected abstract void createSyncedFields();

	public void addSyncedObject(String name, ISyncableObject obj) {
		syncMap.put(name, obj);
	}

	public void sync() {
		Set<ISyncableObject> changed = syncMap.sync();
		if (!changed.isEmpty()) onServerSync(changed);
	}

	public void onServerSync(Set<ISyncableObject> changed) {}

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

}
