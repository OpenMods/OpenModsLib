package openmods.api;

import net.minecraft.entity.player.EntityPlayer;

public interface IHasGui {
	Object getServerGui(EntityPlayer player);

	Object getClientGui(EntityPlayer player);

	boolean canOpenGui(EntityPlayer player);
}
