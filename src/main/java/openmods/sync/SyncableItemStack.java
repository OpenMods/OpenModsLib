package openmods.sync;

import java.io.*;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import openmods.utils.ByteUtils;

import org.apache.commons.compress.utils.BoundedInputStream;

public class SyncableItemStack extends SyncableObjectBase {

	private ItemStack stack;

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
		int length = ByteUtils.readVLI(stream);
		if (length > 0) {
			// GZIP stream reads more than needed -> needs bounding if we want to reuse stream
			NBTTagCompound serialized = CompressedStreamTools.readCompressed(new BoundedInputStream(stream, length));
			this.stack = ItemStack.loadItemStackFromNBT(serialized);
		} else {
			this.stack = null;
		}
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		if (stack != null) {
			NBTTagCompound serialized = new NBTTagCompound();
			stack.writeToNBT(serialized);

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			CompressedStreamTools.writeCompressed(serialized, buffer);

			byte[] bytes = buffer.toByteArray();
			ByteUtils.writeVLI(stream, bytes.length);
			stream.write(bytes);
		} else {
			stream.writeByte(0);
		}

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		if (stack != null) {
			NBTTagCompound serialized = new NBTTagCompound();
			stack.writeToNBT(serialized);
			nbt.setTag(name, serialized);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		if (nbt.hasKey(name)) {
			NBTTagCompound serialized = nbt.getCompoundTag(name);
			stack = ItemStack.loadItemStackFromNBT(serialized);
		} else {
			stack = null;
		}
	}

	public void set(ItemStack stack) {
		this.stack = stack != null? stack.copy() : null;
		markDirty();
	}

	public ItemStack get() {
		return stack;
	}
}
