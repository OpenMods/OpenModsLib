package openmods.inventory.transfer.sinks;

import openmods.inventory.transfer.IRevertable;
import net.minecraft.item.ItemStack;

public interface IItemStackSink extends IRevertable {
	public int accept(ItemStack item);
}
