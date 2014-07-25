package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

import com.google.common.primitives.SignedBytes;

public class SyncableByte extends SyncableObjectBase implements ISyncableValueProvider<Byte> {

	private byte value;

	public SyncableByte(byte value) {
		this.value = value;
	}

	public SyncableByte() {}

	public void set(byte newValue) {
		if (newValue != value) {
			value = newValue;
			markDirty();
		}
	}

	public byte get() {
		return value;
	}

	@Override
	public Byte getValue() {
		return value;
	}

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
		value = stream.readByte();
	}

	@Override
	public void writeToStream(DataOutputStream stream, boolean fullData) throws IOException {
		stream.writeByte(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setByte(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		value = tag.getByte(name);
	}

	public void modify(int by) {
		set(SignedBytes.checkedCast(value + by));
	}
}
