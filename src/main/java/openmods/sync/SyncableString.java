package openmods.sync;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.google.common.base.Objects;

public class SyncableString extends SyncableObjectBase implements ISyncableValueProvider<String> {

	private String value;

	public SyncableString() {
		this("");
	}

	public SyncableString(String val) {
		this.value = val;
	}

	public void setValue(String val) {
		if (!Objects.equal(val, value)) {
			value = val;
			markDirty();
		}
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void readFromStream(PacketBuffer stream) {
		value = stream.readStringFromBuffer(32767);
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeString(value);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		nbt.setString(name, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		value = nbt.getString(name);
	}

	public void clear() {
		setValue("");
	}

}
