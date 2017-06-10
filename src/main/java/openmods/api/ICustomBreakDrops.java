package openmods.api;

import java.util.List;
import net.minecraft.item.ItemStack;

public interface ICustomBreakDrops {
	public List<ItemStack> getDrops(List<ItemStack> originalDrops);
}
