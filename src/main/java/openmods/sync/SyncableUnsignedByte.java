package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

import com.google.common.primitives.UnsignedBytes;

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
	public void readFromStream(DataInputStream stream) throws IOException {
		value = stream.readUnsignedByte();
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
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
