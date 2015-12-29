package openmods.sync;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.google.common.base.Preconditions;

public class SyncableVarInt extends SyncableObjectBase implements ISyncableValueProvider<Integer> {

	private int value = 0;

	public SyncableVarInt(int value) {
		Preconditions.checkArgument(value >= 0, "Value must be non-negative");
		this.value = value;
	}

	public SyncableVarInt() {}

	public void modify(int by) {
		if (value + by >= 0) set(value + by);
	}

	public void set(int val) {
		Preconditions.checkArgument(val >= 0, "Value must be non-negative");
		if (val != value) {
			value = val;
			markDirty();
		}
	}

	public int get() {
		return value;
	}

	@Override
	public Integer getValue() {
		return value;
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readVarIntFromBuffer();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeVarIntToBuffer(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setInteger(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		if (tag.hasKey(name)) value = tag.getInteger(name);
	}

}
