package openmods.gui.component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import openmods.utils.render.FakeIcon;

public class GuiComponentYouTube extends BaseComponent {

	private static final ResourceLocation texture = new ResourceLocation("openmodslib:textures/gui/book.png");
	public static IIcon icon = FakeIcon.createSheetIcon(0, 236, 12, 8);

	private final int PROMPT_REPLY_ACTION = 0;

	private GuiComponentLabel label;
	private GuiComponentSprite image;
	private String url;

	public static URI youtubeUrl;

	public GuiComponentYouTube(int x, int y, String url) {
		super(x, y);
		label = new GuiComponentLabel(15, 2, "Watch video");
		label.setScale(0.5f);
		image = new GuiComponentSprite(0, 0, icon, texture);
		addComponent(label);
		addComponent(image);
		this.url = url;
	}

	@Override
	public int getWidth() {
		return 50;
	}

	@Override
	public int getHeight() {
		return 8;
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		URI uri = URI.create(url);
		if (uri != null) {
			if (Minecraft.getMinecraft().gameSettings.chatLinksPrompt) {
				youtubeUrl = uri;
				Minecraft.getMinecraft().displayGuiScreen(new GuiConfirmOpenLink(Minecraft.getMinecraft().currentScreen, url, PROMPT_REPLY_ACTION, false));
			} else {
				openURI(uri);
			}
		}
	}

	public static void openURI(URI uri) {
		try {
			Desktop.getDesktop().browse(uri);
		} catch (IOException e) {}
	}

}
