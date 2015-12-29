package openmods.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * Custom callback with all information, but called after block is placed and intialized and only for ItemOpenBlock
 */
public interface IPlaceAwareTile {
	public void onBlockPlacedBy(EntityPlayer player, EnumFacing side, ItemStack stack, float hitX, float hitY, float hitZ);
}
