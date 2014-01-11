package openmods.container;

import net.minecraft.inventory.IInventory;

public abstract class ContainerInventory<T extends IInventory> extends ContainerBase<T> {

	public ContainerInventory(IInventory playerInventory, T owner) {
		super(playerInventory, owner, owner);
	}
}
