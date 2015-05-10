package openmods.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Custom callback with all information, but called after block is placed and intialized and only for ItemOpenBlock
 */
public interface IPlaceAwareTile {
	public void onBlockPlacedBy(EntityPlayer player, ForgeDirection side, ItemStack stack, float hitX, float hitY, float hitZ);
}
