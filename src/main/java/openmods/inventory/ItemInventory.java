package openmods.inventory;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public class ItemInventory extends GenericInventory {

	public static final String TAG_INVENTORY = "inventory";

	@Nonnull
	protected final ItemStack containerStack;

	public ItemInventory(@Nonnull ItemStack containerStack, int size) {
		super(size);
		Preconditions.checkNotNull(containerStack);
		this.containerStack = containerStack;
		final CompoundNBT tag = containerStack.getOrCreateTag();
		readFromNBT(getInventoryTag(tag));

	}

	@Override
	public void onInventoryChanged(int slotNumber) {
		super.onInventoryChanged(slotNumber);

		CompoundNBT tag = containerStack.getOrCreateTag();
		CompoundNBT inventoryTag = getInventoryTag(tag);
		writeToNBT(inventoryTag);
		tag.put(TAG_INVENTORY, inventoryTag);
		containerStack.setTag(tag);
	}

	public static CompoundNBT getInventoryTag(CompoundNBT tag) {
		return tag.getCompound(TAG_INVENTORY);
	}

}
