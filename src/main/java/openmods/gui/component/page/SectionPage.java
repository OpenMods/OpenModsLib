package openmods.gui.component.page;

import net.minecraft.util.StatCollector;
import openmods.gui.component.GuiComponentLabel;

public class SectionPage extends PageBase {

	public SectionPage(String name) {
		String txt = StatCollector.translateToLocal(name);
		GuiComponentLabel title = new GuiComponentLabel(0, 0, getWidth(), 40, txt);
		title.setScale(2f);
		title.setX((getWidth() - title.getWidth()) / 2);
		title.setY((getHeight() - title.getHeight()) / 2);
		addComponent(title);
	}
}
