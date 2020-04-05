package openmods.sync;

import java.io.IOException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

/***
 * Note: you must manually .markDirty() right now
 */
public class SyncableNBT extends SyncableObjectBase implements ISyncableValueProvider<CompoundNBT> {

	private CompoundNBT tag;

	public SyncableNBT() {
		tag = new CompoundNBT();
	}

	public SyncableNBT(CompoundNBT nbt) {
		tag = nbt.copy();
	}

	@Override
	public CompoundNBT getValue() {
		return tag.copy();
	}

	public void setValue(CompoundNBT tag) {
		this.tag = tag.copy();
	}

	@Override
	public void readFromStream(PacketBuffer stream) throws IOException {
		this.tag = stream.readCompoundTag();

	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeCompoundTag(this.tag);
	}

	@Override
	public void writeToNBT(CompoundNBT nbt, String name) {
		nbt.put(name, nbt);
	}

	@Override
	public void readFromNBT(CompoundNBT nbt, String name) {
		nbt.getCompound(name);
	}

}
