package openmods.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import openmods.api.IValueProvider;
import openmods.inventory.GenericInventory;

public class SyncableInventory extends GenericInventory implements ISyncableObject {

	private boolean dirty = false;

	public SyncableInventory(String name, boolean isInvNameLocalized, int size) {
		super(name, isInvNameLocalized, size);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void markClean() {
		dirty = false;
	}

	@Override
	public void markDirty() {
		dirty = true;
	}

	@Override
	public void readFromStream(DataInputStream stream) throws IOException {
		NBTTagCompound tag = CompressedStreamTools.readCompressed(stream);
		readFromNBT(tag);
	}

	@Override
	public void writeToStream(DataOutputStream stream) throws IOException {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		CompressedStreamTools.writeCompressed(tag, stream);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		this.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		this.readFromNBT(nbt);
	}

}
