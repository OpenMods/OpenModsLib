package openmods.sync;

import java.io.IOException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

/***
 * Note: you must manually .markDirty() right now
 */
public class SyncableNBT extends SyncableObjectBase implements ISyncableValueProvider<NBTTagCompound> {

	private NBTTagCompound tag;

	public SyncableNBT() {
		tag = new NBTTagCompound();
	}

	public SyncableNBT(NBTTagCompound nbt) {
		tag = nbt.copy();
	}

	@Override
	public NBTTagCompound getValue() {
		return tag.copy();
	}

	public void setValue(NBTTagCompound tag) {
		this.tag = tag.copy();
	}

	@Override
	public void readFromStream(PacketBuffer stream) throws IOException {
		this.tag = stream.readCompoundTag();

	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeCompoundTag(this.tag);
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
