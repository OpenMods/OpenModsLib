package openmods.api;

import net.minecraft.inventory.IInventory;
import openmods.utils.OptionalInt;

public interface IInventoryCallback {
	void onInventoryChanged(IInventory inventory, OptionalInt slotNumber);
}
