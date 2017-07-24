package openmods.sync;

import java.io.IOException;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.Constants;

public class SyncableItemStack extends SyncableObjectBase {

	@Nonnull
	private ItemStack stack = ItemStack.EMPTY;

	@Override
	public void readFromStream(PacketBuffer stream) throws IOException {
		this.stack = stream.readItemStack();
	}

	@Override
	public void writeToStream(PacketBuffer stream) {
		stream.writeItemStack(this.stack);

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String name) {
		if (stack.isEmpty()) {
			NBTTagCompound serialized = new NBTTagCompound();
			stack.writeToNBT(serialized);
			nbt.setTag(name, serialized);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String name) {
		if (nbt.hasKey(name, Constants.NBT.TAG_COMPOUND)) {
			NBTTagCompound serialized = nbt.getCompoundTag(name);
			stack = new ItemStack(serialized);
		} else {
			stack = ItemStack.EMPTY;
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
