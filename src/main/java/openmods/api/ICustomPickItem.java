package openmods.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface ICustomPickItem {
	public ItemStack getPickBlock(EntityPlayer player);
}
