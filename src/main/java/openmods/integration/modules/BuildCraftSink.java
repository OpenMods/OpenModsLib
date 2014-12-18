package openmods.integration.modules;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.inventory.transfer.sinks.BufferingSlotSink;
import buildcraft.api.transport.IPipeTile;

public class BuildCraftSink extends BufferingSlotSink {

	private final IPipeTile pipe;
	private final ForgeDirection side;

	public BuildCraftSink(TileEntity pipe, ForgeDirection side) {
		this.pipe = (IPipeTile)pipe;
		this.side = side;
	}

	@Override
	protected void markDirty() {}

	@Override
	protected void pushStack(ItemStack stack) {
		pipe.injectItem(stack, true, side);
	}

	@Override
	protected boolean isValid(ItemStack stack) {
		return pipe.injectItem(stack, false, side) == stack.stackSize;
	}

}
