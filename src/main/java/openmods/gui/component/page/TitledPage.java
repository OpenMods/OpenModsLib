package openmods.gui.component.page;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;
import openmods.gui.component.GuiComponentLabel;

import org.apache.commons.lang3.StringEscapeUtils;

public class TitledPage extends PageBase {

	public TitledPage(String title, String content) {
		String translatedTitle = StatCollector.translateToLocal(title);
		String translatedContent = StringEscapeUtils.unescapeJava(StatCollector.translateToLocal(content));
		float scaleTitle = Float.parseFloat(StatCollector.translateToLocal("openmodslib.locale.scale.title"));
		float scaleContent = Float.parseFloat(StatCollector.translateToLocal("openmodslib.locale.scale.content"));
		int lineSpace = Integer.parseInt(StatCollector.translateToLocal("openmodslib.locale.lineSpace.titledPage"));

		int x = (getWidth() - Minecraft.getMinecraft().fontRenderer.getStringWidth(translatedTitle)) / 2;
		
		addComponent(new GuiComponentLabel(x, 12, translatedTitle).setScale(scaleTitle));

		final GuiComponentLabel lblContent = new GuiComponentLabel(0, 35, getWidth() - 20, 300, translatedContent);
		
		lblContent.setScale(scaleContent);
		lblContent.setAdditionalLineHeight(lineSpace);

		lblContent.setX((getWidth() - lblContent.getWidth()) / 2);
		addComponent(lblContent);
	}
}
