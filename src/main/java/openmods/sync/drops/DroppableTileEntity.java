package openmods.sync.drops;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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

	protected ItemStack getRawDrop(IBlockState blockState) {
		return new ItemStack(blockState.getBlock());
	}

	@Override
	public void addHarvestDrops(EntityPlayer player, List<ItemStack> drops, IBlockState blockState, int fortune, boolean isSilkTouch) {
		drops.add(getDropStack(blockState));
	}

	@Override
	public ItemStack getPickBlock(EntityPlayer player) {
		final IBlockState state = world.getBlockState(pos);
		return getDropStack(state);
	}

	protected ItemStack getDropStack(IBlockState blockState) {
		return getDropSerializer().write(getRawDrop(blockState));
	}

	@Override
	public void onBlockPlacedBy(IBlockState state, EntityLivingBase placer, ItemStack stack) {
		getDropSerializer().read(stack, true);
	}

}
