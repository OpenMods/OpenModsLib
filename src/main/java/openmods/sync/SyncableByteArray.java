package openmods.sync;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class SyncableByteArray extends SyncableObjectBase implements ISyncableValueProvider<byte[]> {

	private byte[] value = new byte[0];

	public SyncableByteArray() {}

	public SyncableByteArray(byte[] val) {
		this.value = val;
	}

	public void setValue(byte[] newValue) {
		if (newValue != value) {
			value = newValue;
			markDirty();
		}
	}

	@Override
	public byte[] getValue() {
		return value;
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readByteArray();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		if (value == null) {
			stream.writeInt(0);
		} else {
			stream.writeByteArray(value);
		}
	}

	@Override
	public void writeToNBT(CompoundNBT nbt, String name) {
		nbt.setByteArray(name, value);
	}

	@Override
	public void readFromNBT(CompoundNBT nbt, String name) {
		nbt.getByteArray(name);
	}

}
