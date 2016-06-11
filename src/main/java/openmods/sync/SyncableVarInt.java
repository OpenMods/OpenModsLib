package openmods.sync;

import com.google.common.base.Preconditions;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import net.minecraft.nbt.NBTTagCompound;
import openmods.utils.ByteUtils;

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
	public void readFromStream(DataInputStream stream) {
		value = ByteUtils.readVLI(stream);
	}

	@Override
	public void writeToStream(DataOutputStream stream) {
		ByteUtils.writeVLI(stream, value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setInteger(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		if (tag.hasKey(name)) value = tag.getInteger(name);
	}

}
