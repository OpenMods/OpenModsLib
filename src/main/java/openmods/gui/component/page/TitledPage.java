package openmods.gui.component.page;

import net.minecraft.util.StatCollector;
import openmods.gui.component.GuiComponentHCenter;
import openmods.gui.component.GuiComponentLabel;
import org.apache.commons.lang3.StringEscapeUtils;

public class TitledPage extends PageBase {

	public TitledPage(String title, String content) {
		{
			final String translatedTitle = StatCollector.translateToLocal(title);
			final GuiComponentLabel titleLabel = new GuiComponentLabel(0, 0, translatedTitle).setScale(BookScaleConfig.getPageTitleScale());
			addComponent(new GuiComponentHCenter(0, 12, getWidth()).addComponent(titleLabel));
		}

		{
			final String translatedContent = StringEscapeUtils.unescapeJava(StatCollector.translateToLocal(content));
			final GuiComponentLabel lblContent = new GuiComponentLabel(0, 0, getWidth() - 20, 300, translatedContent);

			lblContent.setScale(BookScaleConfig.getPageContentScale());
			lblContent.setAdditionalLineHeight(BookScaleConfig.getTitlePageSeparator());

			addComponent(new GuiComponentHCenter(0, 35, getWidth()).addComponent(lblContent));
		}
	}

}
