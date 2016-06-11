package openmods.container;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import openmods.utils.InventoryUtils;

public abstract class ContainerBase<T> extends Container {

	protected final int inventorySize;
	protected final IInventory playerInventory;
	protected final T owner;
	protected final IInventory inventory;

	protected static class RestrictedSlot extends Slot {

		private final int inventoryIndex;

		public RestrictedSlot(IInventory inventory, int slot, int x, int y) {
			super(inventory, slot, x, y);
			inventoryIndex = slot; // since slotIndex is private
		}

		@Override
		public boolean isItemValid(ItemStack itemstack) {
			return inventory.isItemValidForSlot(inventoryIndex, itemstack);
		}
	}

	public ContainerBase(IInventory playerInventory, IInventory ownerInventory, T owner) {
		this.owner = owner;
		this.inventory = ownerInventory;
		this.inventorySize = inventory.getSizeInventory();
		this.playerInventory = playerInventory;
	}

	protected void addInventoryGrid(int xOffset, int yOffset, int width) {
		int height = (int)Math.ceil((double)inventorySize / width);
		for (int y = 0, slotId = 0; y < height; y++) {
			for (int x = 0; x < width; x++, slotId++) {
				addSlotToContainer(new RestrictedSlot(inventory, slotId,
						xOffset + x * 18,
						yOffset + y * 18));
			}
		}
	}

	protected void addInventoryLine(int xOffset, int yOffset, int start, int count) {
		addInventoryLine(xOffset, yOffset, start, count, 0);
	}

	protected void addInventoryLine(int xOffset, int yOffset, int start, int count, int margin) {
		for (int x = 0, slotId = start; x < count; x++, slotId++) {
			addSlotToContainer(new RestrictedSlot(inventory, slotId,
					xOffset + x * (18 + margin),
					yOffset));
		}
	}

	protected void addPlayerInventorySlots(int offsetY) {
		addPlayerInventorySlots(8, offsetY);
	}

	protected void addPlayerInventorySlots(int offsetX, int offsetY) {
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < 9; column++)
				addSlotToContainer(new Slot(playerInventory,
						column + row * 9 + 9,
						offsetX + column * 18,
						offsetY + row * 18));

		for (int slot = 0; slot < 9; slot++)
			addSlotToContainer(new Slot(playerInventory, slot, offsetX + slot
					* 18, offsetY + 58));
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer) {
		return inventory.isUseableByPlayer(entityplayer);
	}

	public T getOwner() {
		return owner;
	}

	protected boolean mergeItemStackSafe(ItemStack stackToMerge, int start, int stop, boolean reverse) {
		boolean inventoryChanged = false;

		final int delta = reverse? -1 : 1;
		List<Slot> slots = getSlots();

		if (stackToMerge.isStackable()) {
			int slotId = reverse? stop - 1 : start;
			while (stackToMerge.stackSize > 0 && ((!reverse && slotId < stop) || (reverse && slotId >= start))) {

				Slot slot = slots.get(slotId);

				if (canTransferItemsIn(slot)) {
					ItemStack stackInSlot = slot.getStack();

					if (InventoryUtils.tryMergeStacks(stackToMerge, stackInSlot)) {
						slot.onSlotChanged();
						inventoryChanged = true;
					}
				}

				slotId += delta;
			}
		}

		if (stackToMerge.stackSize > 0) {
			int slotId = reverse? stop - 1 : start;

			while ((!reverse && slotId < stop) || (reverse && slotId >= start)) {
				Slot slot = slots.get(slotId);
				ItemStack stackInSlot = slot.getStack();

				if (stackInSlot == null && canTransferItemsIn(slot) && slot.isItemValid(stackToMerge)) {
					slot.putStack(stackToMerge.copy());
					slot.onSlotChanged();
					stackToMerge.stackSize = 0;
					return true;
				}

				slotId += delta;
			}
		}

		return inventoryChanged;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotId) {
		Slot slot = (Slot)inventorySlots.get(slotId);

		if (slot != null && canTransferItemOut(slot) && slot.getHasStack()) {
			ItemStack itemToTransfer = slot.getStack();
			ItemStack copy = itemToTransfer.copy();
			if (slotId < inventorySize) {
				if (!mergeItemStackSafe(itemToTransfer, inventorySize, inventorySlots.size(), true)) return null;
			} else if (!mergeItemStackSafe(itemToTransfer, 0, inventorySize, false)) return null;

			if (itemToTransfer.stackSize == 0) slot.putStack(null);
			else slot.onSlotChanged();

			if (itemToTransfer.stackSize != copy.stackSize) return copy;
		}
		return null;
	}

	protected boolean canTransferItemOut(Slot slot) {
		if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).canTransferItemsOut();
		return true;
	}

	protected boolean canTransferItemsIn(Slot slot) {
		if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).canTransferItemsIn();
		return true;
	}

	public int getInventorySize() {
		return inventorySize;
	}

	@SuppressWarnings("unchecked")
	protected List<Slot> getSlots() {
		return inventorySlots;
	}

	@SuppressWarnings("unchecked")
	public Set<EntityPlayer> getPlayers() {
		Set<EntityPlayer> players = new HashSet<EntityPlayer>();
		for (ICrafting crafter : (List<ICrafting>)crafters) {
			if (crafter instanceof EntityPlayerMP) {
				players.add((EntityPlayerMP)crafter);
			}
		}
		return players;
	}

	public void onButtonClicked(EntityPlayer player, int buttonId) {}

	@Override
	public boolean enchantItem(EntityPlayer player, int buttonId) {
		onButtonClicked(player, buttonId);
		return false;
	}

	@Override
	public ItemStack slotClick(int slotId, int key, int modifier, EntityPlayer player) {
		if (slotId >= 0 && slotId < inventorySlots.size()) {
			Slot slot = getSlot(slotId);
			if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).onClick(player, key, modifier);
		}

		return super.slotClick(slotId, key, modifier, player);
	}

	@Override
	public boolean canDragIntoSlot(Slot slot) {
		if (slot instanceof ICustomSlot) return ((ICustomSlot)slot).canDrag();

		return super.canDragIntoSlot(slot);
	}

}
