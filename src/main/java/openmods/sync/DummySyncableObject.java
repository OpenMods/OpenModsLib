package openmods.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import openmods.Log;

public class DummySyncableObject extends SyncableObjectBase {

	public static final DummySyncableObject INSTANCE = new DummySyncableObject();

	@Override
	public void readFromStream(DataInput stream) throws IOException {
		Log.warn("Trying to read dummy syncable object");
	}

	@Override
	public void writeToStream(DataOutput stream, boolean fullData) throws IOException {
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
