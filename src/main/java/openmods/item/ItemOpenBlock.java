package openmods.item;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import openmods.block.OpenBlock;

public class ItemOpenBlock extends ItemBlock {

	public ItemOpenBlock(Block block) {
		super(block);
	}

	protected void afterBlockPlaced(ItemStack stack, EntityPlayer player, World world, BlockPos pos) {
		stack.stackSize--;
	}

	protected boolean isStackValid(ItemStack stack, EntityPlayer player) {
		return stack.stackSize >= 0;
	}

	/**
	 * Replicates the super method, but with our own hooks
	 */
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		final IBlockState clickedBlockState = world.getBlockState(pos);
		final Block clickedBlock = clickedBlockState.getBlock();

		if (!clickedBlock.isReplaceable(world, pos)) pos = pos.offset(side);
		if (!isStackValid(stack, player)) return false;
		if (!player.canPlayerEdit(pos, side, stack)) return false;
		if (!world.canBlockBePlaced(this.block, pos, false, side, (Entity)null, stack)) return false;

		final int itemMetadata = getMetadata(stack.getMetadata());

		if (this.block instanceof OpenBlock) {
			final OpenBlock openBlock = (OpenBlock)this.block;
			if (!openBlock.canBlockBePlaced(world, pos, side, hitX, hitY, hitZ, itemMetadata, player)) return false;
		}

		final IBlockState newBlockState = this.block.onBlockPlaced(world, pos, side, hitX, hitY, hitZ, itemMetadata, player);

		if (placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newBlockState)) {
			world.playSoundEffect(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, this.block.stepSound.getPlaceSound(), (this.block.stepSound.getVolume() + 1.0F) / 2.0F, this.block.stepSound.getFrequency() * 0.8F);
			afterBlockPlaced(stack, player, world, pos);
		}

		return true;
	}
}
