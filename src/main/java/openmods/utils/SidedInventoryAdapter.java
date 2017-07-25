package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import openmods.utils.bitmap.IReadableBitMap;

public class SidedInventoryAdapter implements ISidedInventory {

	private final IInventory inventory;

	private class SlotInfo {
		private final IReadableBitMap<EnumFacing> sideFlags;
		private final boolean canInsert;
		private final boolean canExtract;

		private SlotInfo(IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
			this.sideFlags = sideFlags;
			this.canInsert = canInsert;
			this.canExtract = canExtract;
		}

		private boolean canAccessFromSide(EnumFacing side) {
			return sideFlags.get(side);
		}
	}

	private final Map<Integer, SlotInfo> slots = Maps.newHashMap();

	public SidedInventoryAdapter(IInventory inventory) {
		this.inventory = inventory;
	}

	public void registerSlot(Enum<?> slot, IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		registerSlot(slot.ordinal(), sideFlags, canInsert, canExtract);
	}

	public void registerSlot(int slot, IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		final int sizeInventory = inventory.getSizeInventory();
		Preconditions.checkArgument(slot >= 0 && slot < sizeInventory, "Tried to register invalid slot: %s (inventory size: %s)", slot, sizeInventory);
		slots.put(slot, new SlotInfo(sideFlags, canInsert, canExtract));
	}

	public void registerSlots(int start, int count, IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = start; i < start + count; i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	public void registerAllSlots(IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = 0; i < inventory.getSizeInventory(); i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	@Override
	public int getSizeInventory() {
		return inventory.getSizeInventory();
	}

	@Override
	@Nonnull
	public ItemStack getStackInSlot(int i) {
		return inventory.getStackInSlot(i);
	}

	@Override
	@Nonnull
	public ItemStack decrStackSize(int i, int j) {
		return inventory.decrStackSize(i, j);
	}

	@Override
	@Nonnull
	public ItemStack removeStackFromSlot(int slot) {
		return inventory.removeStackFromSlot(slot);
	}

	@Override
	public void setInventorySlotContents(int i, @Nonnull ItemStack itemstack) {
		inventory.setInventorySlotContents(i, itemstack);
	}

	@Override
	public int getInventoryStackLimit() {
		return inventory.getInventoryStackLimit();
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer entityplayer) {
		return inventory.isUsableByPlayer(entityplayer);
	}

	@Override
	public boolean isItemValidForSlot(int i, @Nonnull ItemStack itemstack) {
		return inventory.isItemValidForSlot(i, itemstack);
	}

	@Override
	public int[] getSlotsForFace(EnumFacing dir) {
		Set<Integer> result = Sets.newHashSet();
		for (Entry<Integer, SlotInfo> entry : slots.entrySet()) {
			if (entry.getValue().canAccessFromSide(dir)) result.add(entry.getKey());
		}

		return Ints.toArray(result);
	}

	@Override
	public boolean canInsertItem(int slotIndex, @Nonnull ItemStack itemstack, EnumFacing dir) {
		SlotInfo slot = slots.get(slotIndex);
		if (slot == null) return false;
		return slot.canInsert && slot.canAccessFromSide(dir) && inventory.isItemValidForSlot(slotIndex, itemstack);
	}

	@Override
	public boolean canExtractItem(int slotIndex, @Nonnull ItemStack itemstack, EnumFacing dir) {
		SlotInfo slot = slots.get(slotIndex);
		if (slot == null) return false;
		return slot.canExtract && slot.canAccessFromSide(dir);
	}

	@Override
	public String getName() {
		return inventory.getName();
	}

	@Override
	public boolean hasCustomName() {
		return inventory.hasCustomName();
	}

	@Override
	public void markDirty() {
		inventory.markDirty();
	}

	@Override
	public void openInventory(EntityPlayer player) {
		inventory.openInventory(player);
	}

	@Override
	public void closeInventory(EntityPlayer player) {
		inventory.closeInventory(player);
	}

	@Override
	public int getField(int id) {
		return inventory.getField(id);
	}

	@Override
	public void setField(int id, int value) {
		inventory.setField(id, value);
	}

	@Override
	public int getFieldCount() {
		return inventory.getFieldCount();
	}

	@Override
	public void clear() {
		inventory.getFieldCount();
	}

	@Override
	public ITextComponent getDisplayName() {
		return inventory.getDisplayName();
	}

	@Override
	public boolean isEmpty() {
		return inventory.isEmpty();
	}
}
