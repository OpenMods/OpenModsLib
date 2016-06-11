package openmods.sync;

import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import openmods.utils.ByteUtils;

/***
 * Note: you must manually .markDirty() right now
 */
public class SyncableNBT extends SyncableObjectBase implements ISyncableValueProvider<NBTTagCompound> {

	private NBTTagCompound tag;

	public SyncableNBT() {
		tag = new NBTTagCompound();
	}

	public SyncableNBT(NBTTagCompound nbt) {
		tag = (NBTTagCompound)nbt.copy();
	}

	@Override
	public NBTTagCompound getValue() {
		return (NBTTagCompound)tag.copy();
	}

	public void setValue(NBTTagCompound tag) {
		this.tag = (NBTTagCompound)tag.copy();
	}

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
		int length = ByteUtils.readVLI(stream);
		if (length > 0) {
			tag = CompressedStreamTools.readCompressed(ByteStreams.limit(stream, length));
		} else {
			tag = null;
		}

	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		if (tag != null) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			CompressedStreamTools.writeCompressed(tag, buffer);

			byte[] bytes = buffer.toByteArray();
			ByteUtils.writeVLI(stream, bytes.length);
			stream.write(bytes);
		} else {
			stream.writeByte(0);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		nbt.setTag(name, nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		nbt.getCompoundTag(name);
	}

}
