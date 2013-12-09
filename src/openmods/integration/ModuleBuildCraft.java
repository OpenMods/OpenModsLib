package openmods.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import buildcraft.api.transport.IPipeTile;

public class ModuleBuildCraft  {

	private static ModuleBuildCraft _instance = new ModuleBuildCraft();
	
	/* I don't like this, but it's better than offering public access to the instance */
	public static void live() {
		if(!(_instance instanceof ModuleBuildCraftLive)) {
			_instance = new ModuleBuildCraftLive();
		}
	}
	
	public int tryAcceptIntoPipe(TileEntity possiblePipe, ItemStack nextStack, boolean doInsert, ForgeDirection direction) {
		return 0;
	}

	public int tryAcceptIntoPipe(TileEntity possiblePipe, FluidStack nextStack, ForgeDirection direction) {
		return 0;
	}

	public boolean isPipe(TileEntity tile) {
		return false;
	}
	
	public static ModuleBuildCraft instance() {
		return _instance;
	}
	
	private static class ModuleBuildCraftLive extends ModuleBuildCraft {
		
		public int tryAcceptIntoPipe(TileEntity possiblePipe, ItemStack nextStack, boolean doInsert, ForgeDirection direction) {
			if(possiblePipe instanceof IPipeTile) { return ((IPipeTile)possiblePipe).injectItem(nextStack, doInsert, direction.getOpposite()); }
			return 0;
		}

		public int tryAcceptIntoPipe(TileEntity possiblePipe, FluidStack nextStack, ForgeDirection direction) {
			if (possiblePipe instanceof IPipeTile) { return ((IPipeTile)possiblePipe).fill(direction.getOpposite(), nextStack, true); }
			return 0;
		}

		public boolean isPipe(TileEntity tile) {
			return tile instanceof IPipeTile;
		}
	}
}
