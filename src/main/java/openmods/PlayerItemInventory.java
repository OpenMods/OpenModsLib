package openmods;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerItemInventory extends ItemInventory {

	private final EntityPlayer player;
	private final int inventorySlot;

	public PlayerItemInventory(EntityPlayer player, int size) {
		this(player, size, player.inventory.currentItem);
	}

	public PlayerItemInventory(EntityPlayer player, int size, int inventorySlot) {
		super(player.inventory.getStackInSlot(inventorySlot), size);
		this.player = player;
		this.inventorySlot = inventorySlot;
	}

	@Override
	public void onInventoryChanged(int slotNumber) {
		super.onInventoryChanged(slotNumber);
		player.inventory.setInventorySlotContents(inventorySlot, containerStack);
	}
}
