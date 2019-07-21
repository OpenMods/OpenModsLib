package openmods.sync;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class SyncableBoolean extends SyncableObjectBase implements ISyncableValueProvider<Boolean> {

	private boolean value;

	public SyncableBoolean(boolean value) {
		this.value = value;
	}

	public SyncableBoolean() {}

	public void set(boolean newValue) {
		if (newValue != value) {
			value = newValue;
			markDirty();
		}
	}

	public boolean get() {
		return value;
	}

	@Override
	public Boolean getValue() {
		return value;
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readBoolean();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeBoolean(value);
	}

	@Override
	public void writeToNBT(CompoundNBT tag, String name) {
		tag.setBoolean(name, value);
	}

	@Override
	public void readFromNBT(CompoundNBT tag, String name) {
		value = tag.getBoolean(name);
	}

	public void toggle() {
		value = !value;
		markDirty();
	}
}
