package openmods.inventory;

import net.minecraft.entity.player.EntityPlayer;
import openmods.Log;

public class PlayerItemInventory extends ItemInventory {

	private final EntityPlayer player;
	private final int inventorySlot;

	// Potentially unsecure: player can switch item before inventory opens
	public PlayerItemInventory(EntityPlayer player, int size) {
		this(player, size, player.inventory.currentItem);
	}

	public PlayerItemInventory(EntityPlayer player, int size, int inventorySlot) {
		super(player.inventory.getStackInSlot(inventorySlot), size);
		this.player = player;
		this.inventorySlot = inventorySlot;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		if (!(player == this.player && player.inventory.currentItem == inventorySlot)) {
			Log.info("Player %s tried to trigger item duplication bug", player);
			return false;
		}

		return true;
	}

	@Override
	public void onInventoryChanged(int slotNumber) {
		super.onInventoryChanged(slotNumber);
		player.inventory.setInventorySlotContents(inventorySlot, containerStack);
	}
}
