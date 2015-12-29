package openmods.gui.component.page;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import openmods.Log;
import openmods.gui.Icon;
import openmods.gui.component.*;
import openmods.gui.listener.IMouseDownListener;

import org.apache.logging.log4j.Level;

public abstract class PageBase extends BaseComposite {

	public enum ActionIcon {
		YOUTUBE(Icon.createSheetIcon(0, 236, 12, 8)),
		FOLDER(Icon.createSheetIcon(12, 236, 12, 8));

		private ActionIcon(IIcon icon) {
			this.icon = icon;
		}

		public final IIcon icon;
	}

	public static final PageBase BLANK_PAGE = new PageBase() {};

	public static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("openmodslib:textures/gui/book.png");

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

	@Override
	protected void renderComponentBackground(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	public interface IConfirmListener {
		public void onConfirm();
	}

	protected static BaseComponent createActionButton(int x, int y, final String link, IIcon icon, String text, final IConfirmListener listener) {
		EmptyComposite result = new EmptyComposite(x, y, 50, 8);

		GuiComponentLabel label = new GuiComponentLabel(15, 2, StatCollector.translateToLocal(text));
		label.setScale(BookScaleConfig.getPageContentScale());
		result.addComponent(label);

		GuiComponentSprite image = new GuiComponentSprite(0, 0, icon, BOOK_TEXTURE);
		result.addComponent(image);

		result.setListener(new IMouseDownListener() {
			@Override
			public void componentMouseDown(BaseComponent component, int x, int y, int button) {
				final Minecraft mc = Minecraft.getMinecraft();
				if (mc.gameSettings.chatLinksPrompt) {
					final GuiScreen prevGui = mc.currentScreen;
					mc.displayGuiScreen(new GuiConfirmOpenLink(new GuiYesNoCallback() {
						@Override
						public void confirmClicked(boolean result, int id) {
							if (result) listener.onConfirm();
							mc.displayGuiScreen(prevGui);
						}
					}, link, 0, false));
				} else {
					listener.onConfirm();
				}
			}
		});

		return result;
	}

	public PageBase addActionButton(int x, int y, final String link, IIcon icon, String text) {
		addComponent(createActionButton(x, y, link.toString(), icon, text, new IConfirmListener() {
			@Override
			public void onConfirm() {
				final URI uri = URI.create(link);
				try {
					Desktop.getDesktop().browse(uri);
				} catch (IOException e) {
					Log.log(Level.INFO, e, "Failed to open URI '%s'", uri);
				}
			}
		}));

		return this;
	}

	public PageBase addActionButton(int x, int y, final File file, IIcon icon, String text) {
		addComponent(createActionButton(x, y, file.getAbsolutePath(), icon, text, new IConfirmListener() {
			@Override
			public void onConfirm() {
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e) {
					Log.log(Level.INFO, e, "Failed to open file '%s'", file.getAbsolutePath());
				}
			}
		}));

		return this;
	}

}
