package openmods.inventory.transfer.sources;

import openmods.inventory.transfer.IRevertable;
import net.minecraft.item.ItemStack;

public interface IItemStackSource extends IRevertable {
	public ItemStack pull(int amount);

	public ItemStack pullAll();

	public int totalExtracted();
}
