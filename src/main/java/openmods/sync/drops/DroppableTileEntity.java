package openmods.sync.drops;

import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import openmods.api.ICustomHarvestDrops;
import openmods.api.ICustomPickItem;
import openmods.api.IPlaceAwareTile;
import openmods.tileentity.SyncedTileEntity;

public abstract class DroppableTileEntity extends SyncedTileEntity implements IPlaceAwareTile, ICustomHarvestDrops, ICustomPickItem {

	public DroppableTileEntity() {
		getDropSerializer().addFields(this);
	}

	@Override
	public boolean suppressBlockHarvestDrops() {
		return true;
	}

	@Nonnull
	protected ItemStack getRawDrop(BlockState blockState) {
		return new ItemStack(blockState.getBlock());
	}

	@Override
	public void addHarvestDrops(PlayerEntity player, List<ItemStack> drops, BlockState blockState, int fortune, boolean isSilkTouch) {
		drops.add(getDropStack(blockState));
	}

	@Override
	@Nonnull
	public ItemStack getPickBlock(PlayerEntity player) {
		final BlockState state = world.getBlockState(pos);
		return getDropStack(state);
	}

	@Nonnull
	protected ItemStack getDropStack(BlockState blockState) {
		return getDropSerializer().write(getRawDrop(blockState));
	}

	@Override
	public void onBlockPlacedBy(BlockState state, LivingEntity placer, @Nonnull ItemStack stack) {
		getDropSerializer().read(stack, true);
	}

}
