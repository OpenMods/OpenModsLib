package openmods.gui.component.page;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import openmods.gui.component.*;
import openmods.gui.listener.IMouseDownListener;
import openmods.utils.RecipeUtils;
import openmods.utils.render.FakeIcon;

public class StandardRecipePage extends PageBase {

	public static IIcon iconCraftingGrid = FakeIcon.createSheetIcon(0, 180, 56, 56);
	public static IIcon iconArrow = FakeIcon.createSheetIcon(60, 198, 48, 15);
	public static IIcon iconYoutube = FakeIcon.createSheetIcon(0, 236, 12, 8);

	private GuiComponentCraftingGrid craftingGrid;
	private GuiComponentSprite arrow;
	private GuiComponentLabel lblDescription;
	private GuiComponentLabel lblTitle;
	private GuiComponentItemStackSpinner outputSpinner;

	public StandardRecipePage(String title, String description, String videoLink, ItemStack resultingItem) {
		String translatedTitle = StatCollector.translateToLocal(title);
		String translatedDescription = StatCollector.translateToLocal(description).replaceAll("\\\\n", "\n");
		String translatedLink = StatCollector.translateToLocal(videoLink);

		if (videoLink != "" && !videoLink.equals(translatedLink)) {
			addComponent(createYoutubeButton(25, 146, translatedLink));
		}
		lblTitle = new GuiComponentLabel((getWidth() - Minecraft.getMinecraft().fontRenderer.getStringWidth(translatedTitle)) / 2, 12, translatedTitle);
		lblDescription = new GuiComponentLabel(27, 95, 340, 51, translatedDescription);
		arrow = new GuiComponentSprite(90, 50, iconArrow, BOOK_TEXTURE);
		craftingGrid = new GuiComponentCraftingGrid(25, 30, RecipeUtils.getFirstRecipeForItem(resultingItem), iconCraftingGrid, BOOK_TEXTURE);

		lblDescription.setScale(0.5f);
		lblDescription.setAdditionalLineHeight(4);

		outputSpinner = new GuiComponentItemStackSpinner(150, 40, resultingItem);

		addComponent(lblDescription);
		addComponent(lblTitle);
		addComponent(arrow);
		addComponent(outputSpinner);
		addComponent(craftingGrid);

	}

	private static void openURI(URI uri) {
		try {
			Desktop.getDesktop().browse(uri);
		} catch (IOException e) {}
	}

	private static BaseComponent createYoutubeButton(int x, int y, final String link) {
		EmptyComposite result = new EmptyComposite(x, y, 50, 8);

		GuiComponentLabel label = new GuiComponentLabel(15, 2, StatCollector.translateToLocal("openblocks.gui.watch_video"));
		label.setScale(0.5f);
		result.addComponent(label);

		GuiComponentSprite image = new GuiComponentSprite(0, 0, iconYoutube, BOOK_TEXTURE);
		result.addComponent(image);

		final URI uri = URI.create(link);
		result.setListener(new IMouseDownListener() {
			@Override
			public void componentMouseDown(BaseComponent component, int x, int y, int button) {
				final Minecraft mc = Minecraft.getMinecraft();
				if (mc.gameSettings.chatLinksPrompt) {
					final GuiScreen prevGui = mc.currentScreen;
					mc.displayGuiScreen(new GuiConfirmOpenLink(new GuiYesNoCallback() {
						@Override
						public void confirmClicked(boolean result, int id) {
							if (result) openURI(uri);
							mc.displayGuiScreen(prevGui);
						}
					}, link, 0, false));
				} else {
					openURI(uri);
				}
			}
		});

		return result;
	}
}
