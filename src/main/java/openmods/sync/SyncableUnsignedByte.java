package openmods.sync;

import com.google.common.primitives.UnsignedBytes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class SyncableUnsignedByte extends SyncableObjectBase implements ISyncableValueProvider<Integer> {

	private int value;

	public SyncableUnsignedByte(int value) {
		this.value = value;
	}

	public SyncableUnsignedByte() {}

	public void set(int newValue) {
		newValue &= 0xFF;
		if (newValue != value) {
			value = newValue;
			markDirty();
		}
	}

	public int get() {
		return value & 0xFF;
	}

	@Override
	public Integer getValue() {
		return get();
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readUnsignedByte();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeByte(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setByte(name, (byte)(value & 0xFF));
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		value = tag.getByte(name) & 0xFF;
	}

	public void modify(int by) {
		set(UnsignedBytes.saturatedCast(value + by));
	}
}
