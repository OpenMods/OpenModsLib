package openmods.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import openmods.api.IInventoryCallback;

public class GenericInventory implements IInventory {

	public static final String TAG_SLOT = "Slot";
	public static final String TAG_ITEMS = "Items";
	public static final String TAG_SIZE = "size";

	protected List<IInventoryCallback> callbacks;
	protected String inventoryTitle;
	protected int slotsCount;
	protected ItemStack[] inventoryContents;
	protected boolean isInvNameLocalized;

	public GenericInventory(String name, boolean isInvNameLocalized, int size) {
		callbacks = new ArrayList<IInventoryCallback>();
		this.isInvNameLocalized = isInvNameLocalized;
		this.slotsCount = size;
		this.inventoryTitle = name;
		this.inventoryContents = new ItemStack[size];
	}

	public GenericInventory addCallback(IInventoryCallback callback) {
		callbacks.add(callback);
		return this;
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		if (this.inventoryContents[index] != null) {
			ItemStack itemstack;

			if (this.inventoryContents[index].stackSize <= count) {
				itemstack = this.inventoryContents[index];
				this.inventoryContents[index] = null;
				onInventoryChanged(index);
				return itemstack;
			}
			itemstack = this.inventoryContents[index].splitStack(count);
			if (this.inventoryContents[index].stackSize == 0) {
				this.inventoryContents[index] = null;
			}

			onInventoryChanged(index);
			return itemstack;
		}
		return null;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public int getSizeInventory() {
		return slotsCount;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return this.inventoryContents[i];
	}

	public ItemStack getStackInSlot(Enum<?> i) {
		return getStackInSlot(i.ordinal());
	}

	@Override
	public ItemStack removeStackFromSlot(int slot) {
		if (slot >= this.inventoryContents.length) { return null; }
		if (this.inventoryContents[slot] != null) {
			ItemStack itemstack = this.inventoryContents[slot];
			this.inventoryContents[slot] = null;
			onInventoryChanged(slot);
			return itemstack;
		}
		return null;
	}

	public boolean isItem(int slot, Item item) {
		return inventoryContents[slot] != null
				&& inventoryContents[slot].getItem() == item;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return true;
	}

	public void onInventoryChanged(int slotNumber) {
		for (IInventoryCallback callback : callbacks)
			callback.onInventoryChanged(this, slotNumber);
	}

	public void clearAndSetSlotCount(int amount) {
		this.slotsCount = amount;
		inventoryContents = new ItemStack[amount];
		onInventoryChanged(0);
	}

	@Override
	public void clear() {
		for (int i = 0; i < this.inventoryContents.length; ++i)
			this.inventoryContents[i] = null;
	}

	public void readFromNBT(NBTTagCompound tag) {
		readFromNBT(tag, true);
	}

	public void readFromNBT(NBTTagCompound tag, boolean readSize) {
		if (readSize && tag.hasKey(TAG_SIZE)) {
			this.slotsCount = tag.getInteger(TAG_SIZE);
		}

		final NBTTagList nbttaglist = tag.getTagList(TAG_ITEMS, Constants.NBT.TAG_COMPOUND);
		inventoryContents = new ItemStack[slotsCount];
		for (int i = 0; i < nbttaglist.tagCount(); i++) {
			NBTTagCompound stacktag = nbttaglist.getCompoundTagAt(i);
			int j = stacktag.getByte(TAG_SLOT);
			if (j >= 0 && j < inventoryContents.length) {
				inventoryContents[j] = ItemStack.loadItemStackFromNBT(stacktag);
			}
		}
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		this.inventoryContents[i] = itemstack;

		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
			itemstack.stackSize = getInventoryStackLimit();
		}

		onInventoryChanged(i);
	}

	public void writeToNBT(NBTTagCompound tag) {
		tag.setInteger(TAG_SIZE, getSizeInventory());
		NBTTagList nbttaglist = new NBTTagList();
		for (int i = 0; i < inventoryContents.length; i++) {
			if (inventoryContents[i] != null) {
				NBTTagCompound stacktag = new NBTTagCompound();
				inventoryContents[i].writeToNBT(stacktag);
				stacktag.setByte(TAG_SLOT, (byte)i);
				nbttaglist.appendTag(stacktag);
			}
		}
		tag.setTag(TAG_ITEMS, nbttaglist);
	}

	/**
	 * This bastard never even gets called, so don't rely on it.
	 */
	@Override
	public void markDirty() {
		onInventoryChanged(0);
	}

	public void copyFrom(IInventory inventory) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			if (i < getSizeInventory()) {
				ItemStack stack = inventory.getStackInSlot(i);
				if (stack != null) {
					setInventorySlotContents(i, stack.copy());
				} else {
					setInventorySlotContents(i, null);
				}
			}
		}
	}

	public List<ItemStack> contents() {
		return Arrays.asList(inventoryContents);
	}

	@Override
	public String getName() {
		return this.inventoryTitle;
	}

	@Override
	public boolean hasCustomName() {
		return this.isInvNameLocalized;
	}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public ITextComponent getDisplayName() {
		final String name = getName();
		return hasCustomName()
				? new TextComponentString(name)
				: new TextComponentTranslation(name);
	}

	// TODO: figure if it's usable

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {}

	@Override
	public int getFieldCount() {
		return 0;
	}
}
