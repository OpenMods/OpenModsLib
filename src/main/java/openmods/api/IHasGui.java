package openmods.api;

import net.minecraft.entity.player.PlayerEntity;

public interface IHasGui {
	Object getServerGui(PlayerEntity player);

	Object getClientGui(PlayerEntity player);

	boolean canOpenGui(PlayerEntity player);
}
