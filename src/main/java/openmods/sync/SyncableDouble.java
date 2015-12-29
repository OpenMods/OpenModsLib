package openmods.sync;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class SyncableDouble extends SyncableObjectBase implements ISyncableValueProvider<Double> {

	private double value;

	public SyncableDouble(double value) {
		this.value = value;
	}

	public SyncableDouble() {}

	public void set(double newValue) {
		if (newValue != value) {
			value = newValue;
			markDirty();
		}
	}

	public double get() {
		return value;
	}

	@Override
	public Double getValue() {
		return value;
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readDouble();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeDouble(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag, String name) {
		tag.setDouble(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag, String name) {
		value = tag.getDouble(name);
	}

	public void modify(float by) {
		set(value + by);
	}
}
