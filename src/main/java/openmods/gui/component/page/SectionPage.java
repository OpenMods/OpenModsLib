package openmods.gui.component.page;

import net.minecraft.util.StatCollector;
import openmods.gui.component.GuiComponentHCenter;
import openmods.gui.component.GuiComponentLabel;
import openmods.gui.component.GuiComponentVCenter;

public class SectionPage extends PageBase {

	public SectionPage(String name) {
		String txt = StatCollector.translateToLocal(name);
		GuiComponentLabel title = new GuiComponentLabel(0, 0, getWidth(), 40, txt);
		title.setScale(BookScaleConfig.getSectionTitleScale());

		addComponent(GuiComponentHCenter.wrap(0, 0, getWidth(),
				GuiComponentVCenter.wrap(0, 0, getHeight(),
						title)));
	}
}
