package openmods.sync;

import com.google.common.base.Objects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

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
		value = stream.readString(Short.MAX_VALUE);
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
