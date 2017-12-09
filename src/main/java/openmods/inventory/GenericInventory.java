package openmods.inventory;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import openmods.api.IInventoryCallback;
import openmods.utils.OptionalInt;

public class GenericInventory implements IInventory {

	public static final String TAG_SLOT = "Slot";
	public static final String TAG_ITEMS = "Items";
	public static final String TAG_SIZE = "size";

	protected List<IInventoryCallback> callbacks;
	protected String inventoryTitle;
	protected int slotsCount;
	protected NonNullList<ItemStack> inventoryContents;
	protected boolean isInvNameLocalized;
	private IItemHandlerModifiable handler;

	public GenericInventory(String name, boolean isInvNameLocalized, int size) {
		callbacks = new ArrayList<>();
		this.isInvNameLocalized = isInvNameLocalized;
		this.slotsCount = size;
		this.inventoryTitle = name;
		this.inventoryContents = NonNullList.withSize(size, ItemStack.EMPTY);
	}

	public GenericInventory addCallback(IInventoryCallback callback) {
		callbacks.add(callback);
		return this;
	}

	@Override
	@Nonnull
	public ItemStack decrStackSize(int index, int count) {
		final ItemStack result = ItemStackHelper.getAndSplit(this.inventoryContents, index, count);

		if (!result.isEmpty())
			onInventoryChanged(index);

		return result;
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
	@Nonnull
	public ItemStack getStackInSlot(int index) {
		if (index >= 0 && index < this.inventoryContents.size())
			return this.inventoryContents.get(index);
		else
			return ItemStack.EMPTY;
	}

	@Nonnull
	public ItemStack getStackInSlot(Enum<?> i) {
		return getStackInSlot(i.ordinal());
	}

	@Override
	@Nonnull
	public ItemStack removeStackFromSlot(int index) {
		if (index >= this.inventoryContents.size()) return ItemStack.EMPTY;

		final ItemStack result = this.inventoryContents.get(index);
		if (result.isEmpty()) return ItemStack.EMPTY;

		this.inventoryContents.set(index, ItemStack.EMPTY);
		return result;
	}

	public boolean isItem(int slot, Item item) {
		return inventoryContents.get(slot).getItem() == item;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer entityplayer) {
		return true;
	}

	public void onInventoryChanged(int slotNumber) {
		onInventoryChanged(OptionalInt.of(slotNumber));
	}

	public void onInventoryChanged(OptionalInt slotNumber) {
		for (IInventoryCallback callback : callbacks)
			callback.onInventoryChanged(this, slotNumber);
	}

	public void clearAndSetSlotCount(int size) {
		this.slotsCount = size;
		inventoryContents = NonNullList.withSize(size, ItemStack.EMPTY);
		onInventoryChanged(OptionalInt.ABSENT);
	}

	@Override
	public void clear() {
		inventoryContents.clear();
	}

	public void readFromNBT(NBTTagCompound tag) {
		readFromNBT(tag, true);
	}

	public void readFromNBT(NBTTagCompound tag, boolean readSize) {
		if (readSize && tag.hasKey(TAG_SIZE)) {
			this.slotsCount = tag.getInteger(TAG_SIZE);
		}

		final NBTTagList nbttaglist = tag.getTagList(TAG_ITEMS, Constants.NBT.TAG_COMPOUND);
		inventoryContents = NonNullList.withSize(this.slotsCount, ItemStack.EMPTY);
		for (int i = 0; i < nbttaglist.tagCount(); i++) {
			NBTTagCompound stacktag = nbttaglist.getCompoundTagAt(i);
			int j = stacktag.getByte(TAG_SLOT);
			if (j >= 0 && j < inventoryContents.size()) {
				final ItemStack stack = new ItemStack(stacktag);
				if (!stack.isEmpty()) inventoryContents.set(j, stack);
			}
		}
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack itemstack) {
		this.inventoryContents.set(index, itemstack);

		if (itemstack.getCount() > getInventoryStackLimit()) {
			itemstack.setCount(getInventoryStackLimit());
		}

		onInventoryChanged(index);
	}

	public void writeToNBT(NBTTagCompound tag) {
		tag.setInteger(TAG_SIZE, getSizeInventory());
		NBTTagList nbttaglist = new NBTTagList();
		for (int i = 0; i < inventoryContents.size(); i++) {
			final ItemStack stack = inventoryContents.get(i);
			if (!stack.isEmpty()) {
				NBTTagCompound stacktag = new NBTTagCompound();
				stack.writeToNBT(stacktag);
				stacktag.setByte(TAG_SLOT, (byte)i);
				nbttaglist.appendTag(stacktag);
			}
		}
		tag.setTag(TAG_ITEMS, nbttaglist);
	}

	@Override
	public void markDirty() {
		onInventoryChanged(OptionalInt.ABSENT);
	}

	public void copyFrom(IInventory inventory) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			if (i < getSizeInventory()) {
				ItemStack stack = inventory.getStackInSlot(i);
				setInventorySlotContents(i, stack.copy());
			}
		}
	}

	public NonNullList<ItemStack> contents() {
		return inventoryContents;
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

	public IItemHandlerModifiable getHandler() {
		if (handler == null) handler = new InvWrapper(this);
		return handler;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : inventoryContents)
			if (!stack.isEmpty()) return false;

		return true;
	}
}
