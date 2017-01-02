package openmods.gui.component.page;

import com.google.common.base.Strings;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.StatCollector;
import openmods.gui.Icon;
import openmods.gui.component.GuiComponentCraftingGrid;
import openmods.gui.component.GuiComponentHCenter;
import openmods.gui.component.GuiComponentItemStackSpinner;
import openmods.gui.component.GuiComponentLabel;
import openmods.gui.component.GuiComponentSprite;
import openmods.utils.RecipeUtils;

public class StandardRecipePage extends PageBase {

	public static Icon iconCraftingGrid = Icon.createSheetIcon(BOOK_TEXTURE, 0, 180, 56, 56);
	public static Icon iconArrow = Icon.createSheetIcon(BOOK_TEXTURE, 60, 198, 48, 15);

	public StandardRecipePage(String title, String description, ItemStack resultingItem) {
		addComponent(new GuiComponentSprite(75, 40, iconArrow));
		addComponent(new GuiComponentItemStackSpinner(140, 30, resultingItem));

		{
			final IRecipe recipe = RecipeUtils.getFirstRecipeForItemStack(resultingItem);
			if (recipe != null) {
				ItemStack[][] input = RecipeUtils.getFullRecipeInput(recipe);
				if (input != null) addComponent(new GuiComponentCraftingGrid(10, 20, input, iconCraftingGrid));
			}
		}

		{
			String translatedTitle = StatCollector.translateToLocal(title);
			final GuiComponentLabel titleLabel = new GuiComponentLabel(0, 0, translatedTitle);
			titleLabel.setScale(BookScaleConfig.getPageTitleScale());
			addComponent(new GuiComponentHCenter(0, 2, getWidth()).addComponent(titleLabel));
		}

		{
			String translatedDescription = StatCollector.translateToLocal(description).replaceAll("\\\\n", "\n");
			GuiComponentLabel lblDescription = new GuiComponentLabel(10, 80, getWidth() - 5, 200, translatedDescription);
			lblDescription.setScale(BookScaleConfig.getPageContentScale());
			lblDescription.setAdditionalLineHeight(BookScaleConfig.getRecipePageSeparator());
			addComponent(lblDescription);
		}
	}

	public StandardRecipePage(String title, String description, String videoLink, ItemStack resultingItem) {
		this(title, description, resultingItem);

		if (!Strings.isNullOrEmpty(videoLink)) {
			addActionButton(10, 133, videoLink, ActionIcon.YOUTUBE.icon, "openmodslib.gui.watch_video");
		}
	}

}
