package openmods.sync;

import com.google.common.base.Objects;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import openmods.api.IValueProvider;

public class SyncableUUID extends SyncableObjectBase implements IValueProvider<UUID> {

	private UUID uuid;

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
		if (stream.readBoolean()) {
			long msb = stream.readLong();
			long lsb = stream.readLong();
			this.uuid = new UUID(msb, lsb);
		} else {
			this.uuid = null;
		}
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		if (uuid != null) {
			stream.writeBoolean(true);
			stream.writeLong(uuid.getMostSignificantBits());
			stream.writeLong(uuid.getLeastSignificantBits());
		} else {
			stream.writeBoolean(false);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		if (uuid != null) {
			NBTTagCompound result = new NBTTagCompound();
			result.setLong("MSB", uuid.getMostSignificantBits());
			result.setLong("LSB", uuid.getLeastSignificantBits());
			nbt.setTag(name, result);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		if (nbt.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
			NBTTagCompound data = nbt.getCompoundTag(name);
			long msb = data.getLong("MSB");
			long lsb = data.getLong("LSB");
			this.uuid = new UUID(msb, lsb);
		} else {
			this.uuid = null;
		}
	}

	public void setValue(UUID value) {
		if (!Objects.equal(uuid, value)) {
			this.uuid = value;
			markDirty();
		}
	}

	public void clear() {
		setValue(null);
	}

	@Override
	public UUID getValue() {
		return uuid;
	}
}
