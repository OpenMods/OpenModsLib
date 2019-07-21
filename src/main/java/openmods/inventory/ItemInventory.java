package openmods.inventory;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import openmods.utils.ItemUtils;

public class ItemInventory extends GenericInventory {

	public static final String TAG_INVENTORY = "inventory";

	@Nonnull
	protected final ItemStack containerStack;

	public ItemInventory(@Nonnull ItemStack containerStack, int size) {
		super("", false, size);
		Preconditions.checkNotNull(containerStack);
		this.containerStack = containerStack;
		final CompoundNBT tag = ItemUtils.getItemTag(containerStack);
		readFromNBT(getInventoryTag(tag));

	}

	@Override
	public void onInventoryChanged(int slotNumber) {
		super.onInventoryChanged(slotNumber);

		CompoundNBT tag = ItemUtils.getItemTag(containerStack);
		CompoundNBT inventoryTag = getInventoryTag(tag);
		writeToNBT(inventoryTag);
		tag.setTag(TAG_INVENTORY, inventoryTag);
		containerStack.setTagCompound(tag);
	}

	public static CompoundNBT getInventoryTag(CompoundNBT tag) {
		return tag.getCompoundTag(TAG_INVENTORY);
	}

}
