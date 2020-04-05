package openmods.container;

import javax.annotation.Nullable;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ContainerType;

public abstract class ContainerInventory<T extends IInventory> extends ContainerBase<T> {

	public ContainerInventory(@Nullable ContainerType<?> type, int id, final IInventory playerInventory, T owner) {
		super(type, id, playerInventory, owner, owner);
	}
}
