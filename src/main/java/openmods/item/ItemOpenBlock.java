package openmods.item;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import openmods.block.OpenBlock;

public class ItemOpenBlock extends BlockItem {

	public ItemOpenBlock(Block block) {
		super(block);
	}

	protected void afterBlockPlaced(@Nonnull ItemStack stack, PlayerEntity player, World world, BlockPos pos) {
		stack.shrink(1);
	}

	protected boolean isStackValid(@Nonnull ItemStack stack, PlayerEntity player) {
		return !stack.isEmpty();
	}

	/**
	 * Replicates the super method, but with our own hooks
	 */
	@Override
	public ActionResultType onItemUse(PlayerEntity player, World world, BlockPos pos, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
		final BlockState clickedBlockState = world.getBlockState(pos);
		final Block clickedBlock = clickedBlockState.getBlock();

		if (!clickedBlock.isReplaceable(world, pos)) pos = pos.offset(facing);

		ItemStack stack = player.getHeldItem(hand);

		if (!isStackValid(stack, player)) return ActionResultType.FAIL;
		if (!player.canPlayerEdit(pos, facing, stack)) return ActionResultType.FAIL;
		if (!world.mayPlace(this.block, pos, false, facing, null)) return ActionResultType.FAIL;

		final int itemMetadata = getMetadata(stack.getMetadata());

		if (this.block instanceof OpenBlock) {
			final OpenBlock openBlock = (OpenBlock)this.block;
			if (!openBlock.canBlockBePlaced(world, pos, hand, facing, hitX, hitY, hitZ, itemMetadata, player)) return ActionResultType.FAIL;
		}

		final BlockState newBlockState = this.block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, itemMetadata, player, hand);

		if (placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, newBlockState)) {
			final SoundType soundType = this.block.getSoundType(newBlockState, world, pos, player);
			world.playSound(player, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
			afterBlockPlaced(stack, player, world, pos);
		}

		return ActionResultType.SUCCESS;
	}
}
