package openmods.item;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import openmods.block.OpenBlock;

public class ItemOpenBlock extends ItemBlock {

	public ItemOpenBlock(Block block) {
		super(block);
	}

	protected void afterBlockPlaced(@Nonnull ItemStack stack, EntityPlayer player, World world, BlockPos pos) {
		stack.shrink(1);
	}

	protected boolean isStackValid(@Nonnull ItemStack stack, EntityPlayer player) {
		return !stack.isEmpty();
	}

	/**
	 * Replicates the super method, but with our own hooks
	 */
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		final IBlockState clickedBlockState = world.getBlockState(pos);
		final Block clickedBlock = clickedBlockState.getBlock();

		if (!clickedBlock.isReplaceable(world, pos)) pos = pos.offset(facing);

		ItemStack stack = player.getHeldItem(hand);

		if (!isStackValid(stack, player)) return EnumActionResult.FAIL;
		if (!player.canPlayerEdit(pos, facing, stack)) return EnumActionResult.FAIL;
		if (!world.mayPlace(this.block, pos, false, facing, (Entity)null)) return EnumActionResult.FAIL;

		final int itemMetadata = getMetadata(stack.getMetadata());

		if (this.block instanceof OpenBlock) {
			final OpenBlock openBlock = (OpenBlock)this.block;
			if (!openBlock.canBlockBePlaced(world, pos, hand, facing, hitX, hitY, hitZ, itemMetadata, player)) return EnumActionResult.FAIL;
		}

		final IBlockState newBlockState = this.block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, itemMetadata, player, hand);

		if (placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, newBlockState)) {
			final SoundType soundType = this.block.getSoundType(newBlockState, world, pos, player);
			world.playSound(player, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
			afterBlockPlaced(stack, player, world, pos);
		}

		return EnumActionResult.SUCCESS;
	}
}
