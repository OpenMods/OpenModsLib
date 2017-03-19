package openmods.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

public interface IActivateAwareTile {

	// TODO 1.10 align for new fields: onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ)
	public boolean onBlockActivated(EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ);
}
