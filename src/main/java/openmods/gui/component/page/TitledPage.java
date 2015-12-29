package openmods.gui.component.page;

import net.minecraft.util.StatCollector;
import openmods.gui.IComponentParent;
import openmods.gui.component.GuiComponentLabel;

import org.apache.commons.lang3.StringEscapeUtils;

public class TitledPage extends PageBase {

	public TitledPage(IComponentParent parent, String title, String content) {
		super(parent);
		String translatedTitle = StatCollector.translateToLocal(title);
		String translatedContent = StringEscapeUtils.unescapeJava(StatCollector.translateToLocal(content));
		int x = (getWidth() - parent.getFontRenderer().getStringWidth(translatedTitle)) / 2;

		addComponent(new GuiComponentLabel(parent, x, 12, translatedTitle).setScale(BookScaleConfig.getPageTitleScale()));

		final GuiComponentLabel lblContent = new GuiComponentLabel(parent, 0, 35, getWidth() - 20, 300, translatedContent);

		lblContent.setScale(BookScaleConfig.getPageContentScale());
		lblContent.setAdditionalLineHeight(BookScaleConfig.getTitlePageSeparator());

		lblContent.setX((getWidth() - lblContent.getWidth()) / 2);
		addComponent(lblContent);
	}
}
