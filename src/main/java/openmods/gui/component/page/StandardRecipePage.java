package openmods.gui.component.page;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.StatCollector;
import openmods.gui.IComponentParent;
import openmods.gui.Icon;
import openmods.gui.component.*;
import openmods.utils.RecipeUtils;

import com.google.common.base.Strings;

public class StandardRecipePage extends PageBase {

	public static Icon iconCraftingGrid = Icon.createSheetIcon(BOOK_TEXTURE, 0, 180, 56, 56);
	public static Icon iconArrow = Icon.createSheetIcon(BOOK_TEXTURE, 60, 198, 48, 15);

	public StandardRecipePage(IComponentParent parent, String title, String description, ItemStack resultingItem) {
		super(parent);
		addComponent(new GuiComponentSprite(parent, 75, 40, iconArrow));
		addComponent(new GuiComponentItemStackSpinner(parent, 140, 30, resultingItem));

		{
			final IRecipe recipe = RecipeUtils.getFirstRecipeForItemStack(resultingItem);
			if (recipe != null) {
				ItemStack[][] input = RecipeUtils.getFullRecipeInput(recipe);
				if (input != null) addComponent(new GuiComponentCraftingGrid(parent, 10, 20, input, iconCraftingGrid));
			}
		}

		{
			String translatedTitle = StatCollector.translateToLocal(title);
			final GuiComponentLabel titleLabel = new GuiComponentLabel(parent, 0, 2, translatedTitle);
			titleLabel.setX((getWidth() - titleLabel.getWidth()) / 2);
			titleLabel.setScale(BookScaleConfig.getPageTitleScale());
			addComponent(titleLabel);
		}

		{
			String translatedDescription = StatCollector.translateToLocal(description).replaceAll("\\\\n", "\n");
			GuiComponentLabel lblDescription = new GuiComponentLabel(parent, 10, 80, getWidth() - 5, 200, translatedDescription);
			lblDescription.setScale(BookScaleConfig.getPageContentScale());
			lblDescription.setAdditionalLineHeight(BookScaleConfig.getRecipePageSeparator());
			addComponent(lblDescription);
		}
	}

	public StandardRecipePage(IComponentParent parent, String title, String description, String videoLink, ItemStack resultingItem) {
		this(parent, title, description, resultingItem);

		if (!Strings.isNullOrEmpty(videoLink)) {
			addActionButton(10, 133, videoLink, ActionIcon.YOUTUBE.icon, "openmodslib.gui.watch_video");
		}
	}

}
