package openmods.gui.misc;

import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.Slot;

public interface ISlotBackgroundRenderer {
	public void render(Gui gui, Slot slot);
}
