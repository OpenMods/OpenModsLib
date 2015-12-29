package openmods.gui.component.page;

import net.minecraft.util.StatCollector;
import openmods.gui.IComponentParent;
import openmods.gui.component.GuiComponentLabel;

public class SectionPage extends PageBase {

	public SectionPage(IComponentParent parent, String name) {
		super(parent);
		String txt = StatCollector.translateToLocal(name);
		GuiComponentLabel title = new GuiComponentLabel(parent, 0, 0, getWidth(), 40, txt);
		title.setScale(BookScaleConfig.getSectionTitleScale());
		title.setX((getWidth() - title.getWidth()) / 2);
		title.setY((getHeight() - title.getHeight()) / 2);
		addComponent(title);
	}
}
