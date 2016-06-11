package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.NBTTagCompound;

public interface ISyncableObject {
	public boolean isDirty();

	public void markClean();

	public void markDirty();

	public void readFromStream(DataInputStream stream) throws IOException;

	public void writeToStream(DataOutputStream stream) throws IOException;

	public void writeToNBT(NBTTagCompound nbt, String name);

	public void readFromNBT(NBTTagCompound nbt, String name);
}
