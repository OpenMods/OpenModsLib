package openmods.sync;

import java.io.IOException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public interface ISyncableObject {
	boolean isDirty();

	void markClean();

	void markDirty();

	void readFromStream(PacketBuffer buf) throws IOException;

	void writeToStream(PacketBuffer buf) throws IOException;

	void writeToNBT(CompoundNBT nbt, String name);

	void readFromNBT(CompoundNBT nbt, String name);
}
