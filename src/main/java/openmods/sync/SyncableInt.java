package openmods.sync;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class SyncableInt extends SyncableObjectBase implements ISyncableValueProvider<Integer> {

	protected int value = 0;

	public SyncableInt(int value) {
		this.value = value;
	}

	public SyncableInt() {}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readInt();
	}

	public void modify(int by) {
		set(value + by);
	}

	public void set(int val) {
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
	public void writeToStream(PacketBuffer stream) {
		stream.writeInt(value);
	}

	@Override
	public void writeToNBT(CompoundNBT tag, String name) {
		tag.setInteger(name, value);
	}

	@Override
	public void readFromNBT(CompoundNBT tag, String name) {
		if (tag.hasKey(name)) {
			value = tag.getInteger(name);
		}
	}

}
