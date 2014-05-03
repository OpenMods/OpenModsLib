package openmods.api;

import net.minecraft.inventory.IInventory;

public interface IInventoryContainer {
	public IInventory[] getInternalInventories();
}
