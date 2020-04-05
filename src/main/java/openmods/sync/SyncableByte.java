package openmods.sync;

import com.google.common.primitives.SignedBytes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

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
	public void readFromStream(PacketBuffer stream) {
		value = stream.readByte();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeByte(value);
	}

	@Override
	public void writeToNBT(CompoundNBT tag, String name) {
		tag.putByte(name, value);
	}

	@Override
	public void readFromNBT(CompoundNBT tag, String name) {
		value = tag.getByte(name);
	}

	public void modify(int by) {
		set(SignedBytes.checkedCast(value + by));
	}
}
