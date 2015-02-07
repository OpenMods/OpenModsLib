package openmods.gui.component.page;

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
		addComponent(new GuiComponentSprite(75, 40, iconArrow, BOOK_TEXTURE));
		addComponent(new GuiComponentItemStackSpinner(140, 30, resultingItem));

		{
			final ItemStack[] recipe = RecipeUtils.getFirstRecipeForItem(resultingItem);
			if (recipe != null) addComponent(new GuiComponentCraftingGrid(10, 20, recipe, iconCraftingGrid, BOOK_TEXTURE));
		}

		{
			String translatedTitle = StatCollector.translateToLocal(title);
			final GuiComponentLabel titleLabel = new GuiComponentLabel(0, 2, translatedTitle);
			titleLabel.setX((getWidth() - titleLabel.getWidth()) / 2);
			addComponent(titleLabel);
		}

		{
			String translatedDescription = StatCollector.translateToLocal(description).replaceAll("\\\\n", "\n");
			GuiComponentLabel lblDescription = new GuiComponentLabel(10, 80, getWidth() - 5, 200, translatedDescription);
			lblDescription.setScale(0.5f);
			lblDescription.setAdditionalLineHeight(4);
			addComponent(lblDescription);
		}
	}

	public StandardRecipePage(String title, String description, String videoLink, ItemStack resultingItem) {
		this(title, description, resultingItem);
		String translatedLink = StatCollector.translateToLocal(videoLink);

		if (videoLink != "" && !videoLink.equals(translatedLink)) {
			addActionButton(10, 133, translatedLink, ActionIcon.YOUTUBE.icon, "openblocks.gui.watch_video");
		}
	}

}
