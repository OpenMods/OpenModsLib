package openmods.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;

public class DummyContainer extends Container {
	@Override
	public boolean canInteractWith(PlayerEntity p_75145_1_) {
		return true;
	}
}
