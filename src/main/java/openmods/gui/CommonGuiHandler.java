package openmods.gui;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import openmods.api.IHasGui;
import openmods.block.OpenBlock;

public class CommonGuiHandler implements IGuiHandler {
	protected final IGuiHandler wrappedHandler;

	public CommonGuiHandler(IGuiHandler wrappedHandler) {
		this.wrappedHandler = wrappedHandler;
	}

	public CommonGuiHandler() {
		this.wrappedHandler = null;
	}

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		if (id != OpenBlock.OPEN_MODS_TE_GUI) return wrappedHandler != null? wrappedHandler.getServerGuiElement(id, player, world, x, y, z) : null;
		else {
			TileEntity tile = world.getTileEntity(x, y, z);
			if (tile instanceof IHasGui) return ((IHasGui)tile).getServerGui(player);
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}
}
