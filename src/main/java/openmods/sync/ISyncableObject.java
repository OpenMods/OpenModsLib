package openmods.sync;

import java.io.IOException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public interface ISyncableObject {
	boolean isDirty();

	void markClean();

	void markDirty();

	void readFromStream(PacketBuffer buf) throws IOException;

	void writeToStream(PacketBuffer buf) throws IOException;

	void writeToNBT(NBTTagCompound nbt, String name);

	void readFromNBT(NBTTagCompound nbt, String name);
}
