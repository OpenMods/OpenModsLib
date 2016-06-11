package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.NBTTagCompound;

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
	public void readFromStream(DataInputStream stream) throws IOException {
		value = stream.readBoolean();
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		stream.writeBoolean(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setBoolean(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		value = tag.getBoolean(name);
	}

	public void toggle() {
		value = !value;
		markDirty();
	}
}
