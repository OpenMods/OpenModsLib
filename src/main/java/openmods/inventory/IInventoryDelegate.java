package openmods.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

public interface IInventoryDelegate extends IInventory, IInventoryProvider {
	@Override
	default int getSizeInventory() {
		return getInventory().getSizeInventory();
	}

	@Override
	default boolean isEmpty() {
		return getInventory().isEmpty();
	}

	@Override
	default ItemStack getStackInSlot(int index) {
		return getInventory().getStackInSlot(index);
	}

	@Override
	default ItemStack decrStackSize(int index, int count) {
		return getInventory().decrStackSize(index, count);
	}

	@Override
	default ItemStack removeStackFromSlot(int index) {
		return getInventory().removeStackFromSlot(index);
	}

	@Override
	default void setInventorySlotContents(int index, ItemStack stack) {
		getInventory().setInventorySlotContents(index, stack);
	}

	@Override
	default int getInventoryStackLimit() {
		return getInventory().getInventoryStackLimit();
	}

	@Override
	default void markDirty() {
		getInventory().markDirty();
	}

	@Override
	default boolean isUsableByPlayer(PlayerEntity player) {
		return getInventory().isUsableByPlayer(player);
	}

	@Override
	default void openInventory(PlayerEntity player) {
		getInventory().openInventory(player);
	}

	@Override
	default void closeInventory(PlayerEntity player) {
		getInventory().closeInventory(player);
	}

	@Override
	default boolean isItemValidForSlot(int index, ItemStack stack) {
		return getInventory().isItemValidForSlot(index, stack);
	}

	@Override
	default int getField(int id) {
		return getInventory().getField(id);
	}

	@Override
	default void setField(int id, int value) {
		getInventory().setField(id, value);
	}

	@Override
	default int getFieldCount() {
		return getInventory().getFieldCount();
	}

	@Override
	default void clear() {
		getInventory().clear();
	}

	@Override
	default String getName() {
		return getInventory().getName();
	}

	@Override
	default boolean hasCustomName() {
		return getInventory().hasCustomName();
	}

	@Override
	default ITextComponent getDisplayName() {
		return getInventory().getDisplayName();
	}
}
