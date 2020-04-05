package openmods.sync;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class SyncableShort extends SyncableObjectBase implements ISyncableValueProvider<Short> {

	private short value = 0;

	public SyncableShort(short value) {
		this.value = value;
	}

	public SyncableShort() {}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readShort();
	}

	public void modify(short by) {
		set((short)(value + by));
	}

	public void set(short val) {
		if (val != value) {
			value = val;
			markDirty();
		}
	}

	public short get() {
		return value;
	}

	@Override
	public Short getValue() {
		return value;
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeShort(value);
	}

	@Override
	public void writeToNBT(CompoundNBT tag, String name) {
		tag.putShort(name, value);
	}

	@Override
	public void readFromNBT(CompoundNBT tag, String name) {
		value = tag.getShort(name);
	}
}
