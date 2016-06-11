package openmods.sync;

import com.google.common.base.Objects;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.NBTTagCompound;

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
	public void readFromStream(DataInputStream stream) throws IOException {
		value = stream.readUTF();
	}

	@Override
	public void writeToStream(DataOutputStream stream)
			throws IOException {
		stream.writeUTF(value);
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
