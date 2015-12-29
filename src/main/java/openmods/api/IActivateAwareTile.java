package openmods.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

public interface IActivateAwareTile {
	public boolean onBlockActivated(EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ);
}
