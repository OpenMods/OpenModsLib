package openmods.container;

import javax.annotation.Nullable;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ContainerType;
import openmods.inventory.IInventoryProvider;

public abstract class ContainerInventoryProvider<T extends IInventoryProvider> extends ContainerBase<T> {

	public ContainerInventoryProvider(@Nullable ContainerType<?> type, int id, IInventory playerInventory, T owner) {
		super(type, id, playerInventory, owner.getInventory(), owner);
	}
}
