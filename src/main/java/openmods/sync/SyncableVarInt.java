package openmods.sync;

import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

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
		value = stream.readVarInt();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeVarInt(value);
	}

	@Override
	public void writeToNBT(CompoundNBT tag, String name) {
		tag.setInteger(name, value);
	}

	@Override
	public void readFromNBT(CompoundNBT tag, String name) {
		if (tag.hasKey(name)) value = tag.getInteger(name);
	}

}
