package openmods.sync.drops;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.api.ICustomHarvestDrops;
import openmods.api.ICustomPickItem;
import openmods.api.IPlaceAwareTile;
import openmods.tileentity.SyncedTileEntity;

public abstract class DroppableTileEntity extends SyncedTileEntity implements IPlaceAwareTile, ICustomHarvestDrops, ICustomPickItem {

	public DroppableTileEntity() {
		getDropSerializer().addFields(this);
	}

	@Override
	public boolean suppressNormalHarvestDrops() {
		return true;
	}

	protected ItemStack getRawDrop() {
		return new ItemStack(getBlockType());
	}

	@Override
	public void addHarvestDrops(EntityPlayer player, List<ItemStack> drops) {
		drops.add(getDropStack());
	}

	@Override
	public ItemStack getPickBlock() {
		return getDropStack();
	}

	protected ItemStack getDropStack() {
		return getDropSerializer().write(getRawDrop());
	}

	@Override
	public void onBlockPlacedBy(EntityPlayer player, ForgeDirection side, ItemStack stack, float hitX, float hitY, float hitZ) {
		getDropSerializer().read(stack, true);
	}

}
