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

	public StandardRecipePage(String title, String description, ItemStack resultingItem) {
		addComponent(new GuiComponentSprite(90, 50, iconArrow, BOOK_TEXTURE));
		addComponent(new GuiComponentItemStackSpinner(150, 40, resultingItem));

		{
			final ItemStack[] recipe = RecipeUtils.getFirstRecipeForItem(resultingItem);
			if (recipe != null) addComponent(new GuiComponentCraftingGrid(25, 30, recipe, iconCraftingGrid, BOOK_TEXTURE));
		}

		{
			String translatedTitle = StatCollector.translateToLocal(title);
			addComponent(new GuiComponentLabel((getWidth() - Minecraft.getMinecraft().fontRenderer.getStringWidth(translatedTitle)) / 2, 12, translatedTitle));
		}

		{
			String translatedDescription = StatCollector.translateToLocal(description).replaceAll("\\\\n", "\n");
			GuiComponentLabel lblDescription = new GuiComponentLabel(27, 95, 340, 51, translatedDescription);
			lblDescription.setScale(0.5f);
			lblDescription.setAdditionalLineHeight(4);
			addComponent(lblDescription);
		}
	}

	public StandardRecipePage(String title, String description, String videoLink, ItemStack resultingItem) {
		this(title, description, resultingItem);
		String translatedLink = StatCollector.translateToLocal(videoLink);

		if (videoLink != "" && !videoLink.equals(translatedLink)) {
			addActionButton(25, 146, translatedLink, ActionIcon.YOUTUBE.icon, "openblocks.gui.watch_video");
		}
	}

}
