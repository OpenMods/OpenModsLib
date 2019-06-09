package openmods.inventory;

import net.minecraft.inventory.ISidedInventory;

public interface ISidedInventoryProvider extends IInventoryProvider {
	@Override
	ISidedInventory getInventory();
}
