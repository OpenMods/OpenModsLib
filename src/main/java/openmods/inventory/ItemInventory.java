package openmods.inventory;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import openmods.utils.ItemUtils;

public class ItemInventory extends GenericInventory {

	public static final String TAG_INVENTORY = "inventory";

	@Nonnull
	protected final ItemStack containerStack;

	public ItemInventory(@Nonnull ItemStack containerStack, int size) {
		super("", false, size);
		Preconditions.checkNotNull(containerStack);
		this.containerStack = containerStack;
		final NBTTagCompound tag = ItemUtils.getItemTag(containerStack);
		readFromNBT(getInventoryTag(tag));

	}

	@Override
	public void onInventoryChanged(int slotNumber) {
		super.onInventoryChanged(slotNumber);

		NBTTagCompound tag = ItemUtils.getItemTag(containerStack);
		NBTTagCompound inventoryTag = getInventoryTag(tag);
		writeToNBT(inventoryTag);
		tag.setTag(TAG_INVENTORY, inventoryTag);
		containerStack.setTagCompound(tag);
	}

	public static NBTTagCompound getInventoryTag(NBTTagCompound tag) {
		return tag.getCompoundTag(TAG_INVENTORY);
	}

}
