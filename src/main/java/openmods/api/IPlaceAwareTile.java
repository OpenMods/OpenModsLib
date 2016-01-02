package openmods.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public interface IPlaceAwareTile {
	public void onBlockPlacedBy(IBlockState state, EntityLivingBase placer, ItemStack stack);
}
