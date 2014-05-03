package openmods;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import openmods.utils.ItemUtils;

public class ItemInventory extends GenericInventory {

	public static final String TAG_INVENTORY = "inventory";

	private final EntityPlayer player;
	private final ItemStack inventoryStack;
	private final int inventorySlot;

	public ItemInventory(EntityPlayer _player, int size) {
		this(_player, size, _player.inventory.currentItem);
	}

	public ItemInventory(EntityPlayer _player, int size, int inventorySlot) {
		super("", false, size);
		player = _player;
		this.inventorySlot = inventorySlot;
		inventoryStack = player.inventory.getStackInSlot(inventorySlot);
		final NBTTagCompound tag = ItemUtils.getItemTag(inventoryStack);
		readFromNBT(getInventoryTag(tag));

	}

	@Override
	public void onInventoryChanged(int slotNumber) {
		super.onInventoryChanged(slotNumber);
		ItemStack currentStack = player.inventory.getStackInSlot(inventorySlot);
		if (currentStack == null || !currentStack.isItemEqual(inventoryStack)) {
			player.closeScreen();
			return;
		}
		NBTTagCompound tag = ItemUtils.getItemTag(currentStack);
		NBTTagCompound inventoryTag = getInventoryTag(tag);
		writeToNBT(inventoryTag);
		tag.setTag(TAG_INVENTORY, inventoryTag);
		currentStack.setTagCompound(tag);
		player.inventory.setInventorySlotContents(inventorySlot, currentStack);
	}

	public static NBTTagCompound getInventoryTag(NBTTagCompound tag) {
		return tag.getCompoundTag(TAG_INVENTORY);
	}

}
