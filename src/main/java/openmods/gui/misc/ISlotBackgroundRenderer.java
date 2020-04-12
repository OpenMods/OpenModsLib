package openmods.gui.misc;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.inventory.container.Slot;
import openmods.gui.component.BaseComponent;

public interface ISlotBackgroundRenderer {
	void render(BaseComponent gui, MatrixStack matrixStack, Slot slot);
}
