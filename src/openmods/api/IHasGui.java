package openmods.api;

import net.minecraft.entity.player.EntityPlayer;

public interface IHasGui {
	public Object getServerGui(EntityPlayer player);

	public Object getClientGui(EntityPlayer player);

	public boolean canOpenGui(EntityPlayer player);
}
