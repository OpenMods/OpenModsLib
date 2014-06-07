package openmods.api;

import java.util.List;

import net.minecraft.item.ItemStack;

public interface ISpecialDrops {
	public void addDrops(List<ItemStack> drops, int fortune);
}
