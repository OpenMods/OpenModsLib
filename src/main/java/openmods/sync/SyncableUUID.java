package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

public class SyncableUUID extends SyncableObjectBase {

	private UUID uuid;

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
		long msb = stream.readLong();
		long lsb = stream.readLong();
		uuid = new UUID(msb, lsb);
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		stream.writeLong(uuid.getMostSignificantBits());
		stream.writeLong(uuid.getLeastSignificantBits());
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		NBTTagCompound result = new NBTTagCompound();
		result.setLong("MSB", uuid.getMostSignificantBits());
		result.setLong("LSB", uuid.getLeastSignificantBits());
		nbt.setTag(name, result);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		NBTTagCompound data = nbt.getCompoundTag(name);
		long msb = data.getLong("MSB");
		long lsb = data.getLong("LSB");
		uuid = new UUID(msb, lsb);
	}

	public void setValue(UUID value) {
		this.uuid = value;
		markDirty();
	}

	public UUID getValue() {
		return uuid;
	}
}
