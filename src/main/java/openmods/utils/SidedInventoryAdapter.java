package openmods.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.utils.bitmap.IReadableBitMap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SidedInventoryAdapter implements ISidedInventory {

	private final IInventory inventory;

	private class SlotInfo {
		private final IReadableBitMap<ForgeDirection> sideFlags;
		private final boolean canInsert;
		private final boolean canExtract;

		private SlotInfo(IReadableBitMap<ForgeDirection> sideFlags, boolean canInsert, boolean canExtract) {
			this.sideFlags = sideFlags;
			this.canInsert = canInsert;
			this.canExtract = canExtract;
		}

		private boolean canAccessFromSide(ForgeDirection side) {
			return sideFlags.get(side);
		}
	}

	private final Map<Integer, SlotInfo> slots = Maps.newHashMap();

	public SidedInventoryAdapter(IInventory inventory) {
		this.inventory = inventory;
	}

	public void registerSlot(Enum<?> slot, IReadableBitMap<ForgeDirection> sideFlags, boolean canInsert, boolean canExtract) {
		registerSlot(slot.ordinal(), sideFlags, canInsert, canExtract);
	}

	public void registerSlot(int slot, IReadableBitMap<ForgeDirection> sideFlags, boolean canInsert, boolean canExtract) {
		slots.put(slot, new SlotInfo(sideFlags, canInsert, canExtract));
	}

	public void registerSlots(int start, int count, IReadableBitMap<ForgeDirection> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = start; i < start + count; i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	public void registerAllSlots(IReadableBitMap<ForgeDirection> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = 0; i < inventory.getSizeInventory(); i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	@Override
	public int getSizeInventory() {
		return inventory.getSizeInventory();
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return inventory.getStackInSlot(i);
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		return inventory.decrStackSize(i, j);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		return inventory.getStackInSlotOnClosing(i);
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		inventory.setInventorySlotContents(i, itemstack);
	}

	@Override
	public int getInventoryStackLimit() {
		return inventory.getInventoryStackLimit();
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return inventory.isUseableByPlayer(entityplayer);
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return inventory.isItemValidForSlot(i, itemstack);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		Set<Integer> result = Sets.newHashSet();
		for (Entry<Integer, SlotInfo> entry : slots.entrySet()) {
			if (entry.getValue().canAccessFromSide(dir)) result.add(entry.getKey());
		}

		int tmp[] = new int[result.size()];
		int i = 0;
		for (Integer value : result)
			tmp[i++] = value;
		return tmp;
	}

	@Override
	public boolean canInsertItem(int slotIndex, ItemStack itemstack, int side) {
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		SlotInfo slot = slots.get(slotIndex);
		if (slot == null) return false;
		return slot.canInsert && slot.canAccessFromSide(dir) && inventory.isItemValidForSlot(slotIndex, itemstack);
	}

	@Override
	public boolean canExtractItem(int slotIndex, ItemStack itemstack, int side) {
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		SlotInfo slot = slots.get(slotIndex);
		if (slot == null) return false;
		return slot.canExtract && slot.canAccessFromSide(dir);
	}

	@Override
	public String getInventoryName() {
		return inventory.getInventoryName();
	}

	@Override
	public boolean hasCustomInventoryName() {
		return inventory.hasCustomInventoryName();
	}

	@Override
	public void markDirty() {
		inventory.markDirty();
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}
}
