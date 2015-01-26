package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

public class SyncableInt extends SyncableObjectBase implements ISyncableValueProvider<Integer> {

	protected int value = 0;

	public SyncableInt(int value) {
		this.value = value;
	}

	public SyncableInt() {}

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
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
	public void writeToStream(DataOutputStream stream) throws IOException {
		stream.writeInt(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setInteger(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		if (tag.hasKey(name)) {
			value = tag.getInteger(name);
		}
	}

}
