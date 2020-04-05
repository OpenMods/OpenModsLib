package openmods.inventory;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import openmods.api.IInventoryCallback;
import openmods.utils.OptionalInt;

public class GenericInventory implements IInventory {

	public static final String TAG_SLOT = "Slot";
	public static final String TAG_ITEMS = "Items";
	public static final String TAG_SIZE = "size";

	protected final List<IInventoryCallback> callbacks;
	protected int slotsCount;
	protected NonNullList<ItemStack> inventoryContents;
	private IItemHandlerModifiable handler;

	public GenericInventory(int size) {
		callbacks = new ArrayList<>();
		this.slotsCount = size;
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
	public boolean isUsableByPlayer(PlayerEntity entityplayer) {
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

	public void readFromNBT(CompoundNBT tag) {
		readFromNBT(tag, true);
	}

	public void readFromNBT(CompoundNBT tag, boolean readSize) {
		if (readSize && tag.contains(TAG_SIZE)) {
			this.slotsCount = tag.getInt(TAG_SIZE);
		}

		final ListNBT nbttaglist = tag.getList(TAG_ITEMS, Constants.NBT.TAG_COMPOUND);
		inventoryContents = NonNullList.withSize(this.slotsCount, ItemStack.EMPTY);
		for (int i = 0; i < nbttaglist.size(); i++) {
			CompoundNBT stacktag = nbttaglist.getCompound(i);
			int j = stacktag.getByte(TAG_SLOT);
			if (j >= 0 && j < inventoryContents.size()) {
				final ItemStack stack = ItemStack.read(stacktag);
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

	public void writeToNBT(CompoundNBT tag) {
		tag.putInt(TAG_SIZE, getSizeInventory());
		ListNBT nbttaglist = new ListNBT();
		for (int i = 0; i < inventoryContents.size(); i++) {
			final ItemStack stack = inventoryContents.get(i);
			if (!stack.isEmpty()) {
				CompoundNBT stacktag = new CompoundNBT();
				stack.write(stacktag);
				stacktag.putByte(TAG_SLOT, (byte)i);
				nbttaglist.add(stacktag);
			}
		}
		tag.put(TAG_ITEMS, nbttaglist);
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
	public void openInventory(PlayerEntity player) {}

	@Override
	public void closeInventory(PlayerEntity player) {}

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
