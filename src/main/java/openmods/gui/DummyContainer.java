package openmods.gui;

import javax.annotation.Nullable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;

public class DummyContainer extends Container {
	public DummyContainer(@Nullable ContainerType<?> type, int id) {
		super(type, id);
	}

	@Override
	public boolean canInteractWith(PlayerEntity p_75145_1_) {
		return true;
	}
}
