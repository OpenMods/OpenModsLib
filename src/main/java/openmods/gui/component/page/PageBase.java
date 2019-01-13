package openmods.gui.component.page;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import openmods.Log;
import openmods.OpenMods;
import openmods.gui.Icon;
import openmods.gui.component.BaseComponent;
import openmods.gui.component.BaseComposite;
import openmods.gui.component.EmptyComposite;
import openmods.gui.component.GuiComponentLabel;
import openmods.gui.component.GuiComponentSprite;
import openmods.gui.listener.IMouseDownListener;
import openmods.utils.TranslationUtils;
import org.apache.logging.log4j.Level;

public abstract class PageBase extends BaseComposite {

	public enum ActionIcon {
		YOUTUBE(Icon.createSheetIcon(BOOK_TEXTURE, 0, 236, 12, 8)),
		FOLDER(Icon.createSheetIcon(BOOK_TEXTURE, 12, 236, 12, 8));

		ActionIcon(Icon icon) {
			this.icon = icon;
		}

		public final Icon icon;
	}

	public static final ResourceLocation BOOK_TEXTURE = OpenMods.location("textures/gui/book.png");

	public PageBase() {
		super(0, 15); // x is set on page turn
	}

	@Override
	public int getWidth() {
		return 180;
	}

	@Override
	public int getHeight() {
		return 140;
	}

	public interface IConfirmListener {
		void onConfirm();
	}

	protected BaseComponent createActionButton(int x, int y, final String link, Icon icon, String text, final IConfirmListener listener) {
		EmptyComposite result = new EmptyComposite(x, y, 50, 8);

		GuiComponentLabel label = new GuiComponentLabel(15, 2, TranslationUtils.translateToLocal(text));
		label.setScale(BookScaleConfig.getPageContentScale());
		result.addComponent(label);

		GuiComponentSprite image = new GuiComponentSprite(0, 0, icon);
		result.addComponent(image);

		result.setListener((IMouseDownListener)(component, clickX, clickY, button) -> {
			final Minecraft mc = Minecraft.getMinecraft();
			if (mc.gameSettings.chatLinksPrompt) {
				final GuiScreen prevGui = mc.currentScreen;
				mc.displayGuiScreen(new GuiConfirmOpenLink((response, id) -> {
					if (response) listener.onConfirm();
					mc.displayGuiScreen(prevGui);
				}, link, 0, false));
			} else {
				listener.onConfirm();
			}
		});

		return result;
	}

	public PageBase addActionButton(int x, int y, final String link, Icon icon, String text) {
		addComponent(createActionButton(x, y, link, icon, text, () -> {
			final URI uri = URI.create(link);
			try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) {
				Log.log(Level.INFO, e, "Failed to open URI '%s'", uri);
			}
		}));

		return this;
	}

	public PageBase addActionButton(int x, int y, final File file, Icon icon, String text) {
		addComponent(createActionButton(x, y, file.getAbsolutePath(), icon, text, () -> {
			try {
				Desktop.getDesktop().open(file);
			} catch (IOException e) {
				Log.log(Level.INFO, e, "Failed to open file '%s'", file.getAbsolutePath());
			}
		}));

		return this;
	}

}
