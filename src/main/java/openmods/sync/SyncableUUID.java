package openmods.sync;

import com.google.common.base.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;
import openmods.api.IValueProvider;

public class SyncableUUID extends SyncableObjectBase implements IValueProvider<UUID> {

	private UUID uuid;

	@Override
	public void readFromStream(PacketBuffer stream) {
		if (stream.readBoolean()) {
			this.uuid = stream.readUniqueId();
		} else {
			this.uuid = null;
		}
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		if (uuid != null) {
			stream.writeBoolean(true);
			stream.writeUniqueId(uuid);
		} else {
			stream.writeBoolean(false);
		}
	}

	@Override
	public void writeToNBT(CompoundNBT nbt, String name) {
		if (uuid != null) {
			CompoundNBT result = new CompoundNBT();
			result.putUniqueId(name, uuid);
		}
	}

	@Override
	public void readFromNBT(CompoundNBT nbt, String name) {
		this.uuid = nbt.getUniqueId(name);
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
