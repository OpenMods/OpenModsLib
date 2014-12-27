package openmods.gui.component.page;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import openmods.gui.component.*;
import openmods.utils.RecipeUtils;
import openmods.utils.render.FakeIcon;

public class StandardRecipePage extends PageBase {

	public static IIcon iconCraftingGrid = FakeIcon.createSheetIcon(0, 180, 56, 56);
	public static IIcon iconArrow = FakeIcon.createSheetIcon(60, 198, 48, 15);

	private GuiComponentCraftingGrid craftingGrid;
	private GuiComponentSprite arrow;
	private GuiComponentLabel lblDescription;
	private GuiComponentLabel lblTitle;
	private GuiComponentItemStackSpinner outputSpinner;

	public StandardRecipePage(String title, String description, ItemStack resultingItem) {
		String translatedTitle = StatCollector.translateToLocal(title);
		String translatedDescription = StatCollector.translateToLocal(description).replaceAll("\\\\n", "\n");

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

	public StandardRecipePage(String title, String description, String videoLink, ItemStack resultingItem) {
		this(title, description, resultingItem);
		String translatedLink = StatCollector.translateToLocal(videoLink);

		if (videoLink != "" && !videoLink.equals(translatedLink)) {
			addActionButton(25, 146, translatedLink, ActionIcon.YOUTUBE.icon, "openblocks.gui.watch_video");
		}
	}

}
