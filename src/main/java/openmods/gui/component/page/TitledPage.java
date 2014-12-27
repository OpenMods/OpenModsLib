package openmods.gui.component.page;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;
import openmods.gui.component.GuiComponentLabel;

import org.apache.commons.lang3.StringEscapeUtils;

public class TitledPage extends PageBase {

	public TitledPage(String title, String content) {
		String translatedTitle = StatCollector.translateToLocal(title);
		String translatedContent = StringEscapeUtils.unescapeJava(StatCollector.translateToLocal(content));

		int x = (getWidth() - Minecraft.getMinecraft().fontRenderer.getStringWidth(translatedTitle)) / 2;

		addComponent(new GuiComponentLabel(x, 12, translatedTitle));

		final GuiComponentLabel lblContent = new GuiComponentLabel(27, 40, 300, 300, translatedContent);
		lblContent.setScale(0.5f);
		lblContent.setAdditionalLineHeight(2);
		addComponent(lblContent);
	}
}
