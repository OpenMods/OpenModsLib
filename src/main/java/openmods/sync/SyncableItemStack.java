package openmods.sync;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

public class SyncableItemStack extends SyncableObjectBase {

	private ItemStack stack;

	@Override
	public void readFromStream(PacketBuffer stream) throws IOException {
		this.stack = stream.readItemStackFromBuffer();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeItemStackToBuffer(this.stack);

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
		if (nbt.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
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
