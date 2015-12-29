package openmods.sync;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import openmods.Log;

public class DummySyncableObject extends SyncableObjectBase {

	public static final DummySyncableObject INSTANCE = new DummySyncableObject();

	@Override
	public void readFromStream(PacketBuffer buf) {
		Log.warn("Trying to read dummy syncable object");
	}

	@Override
	public void writeToStream(PacketBuffer buf) {
		Log.warn("Trying to write dummy syncable object");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		Log.warn("Trying to write dummy syncable object");
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		Log.warn("Trying to read dummy syncable object");
	}

}
