package openmods.gui.component;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import openmods.utils.render.FakeIcon;

public class GuiComponentStandardRecipePage extends BaseComponent {

	private static final ResourceLocation texture = new ResourceLocation("openmodslib:textures/gui/book.png");

	public static Icon iconCraftingGrid = FakeIcon.createSheetIcon(0, 180, 56, 56);
	public static Icon iconArrow = FakeIcon.createSheetIcon(60, 198, 48, 15);

	private GuiComponentCraftingGrid craftingGrid;
	private GuiComponentSprite arrow;
	private GuiComponentLabel lblDescription;
	private GuiComponentLabel lblTitle;
	private GuiComponentItemStackSpinner outputSpinner;
	
	public GuiComponentStandardRecipePage(String title, String description, String videoLink, ItemStack resultingItem) {
		super(0, 0);

		String translatedTitle = StatCollector.translateToLocal(title);
		String translatedDescription = StatCollector.translateToLocal(description).replaceAll("\\\\n", "\n");
		String translatedLink = StatCollector.translateToLocal(videoLink);

		if (videoLink != "" && !videoLink.equals(translatedLink)) {
			addComponent(new GuiComponentYouTube(25, 146, translatedLink));
		}
		lblTitle = new GuiComponentLabel((getWidth() - Minecraft.getMinecraft().fontRenderer.getStringWidth(translatedTitle)) / 2, 12, translatedTitle);
		lblDescription = new GuiComponentLabel(27, 95, 340, 51, translatedDescription);
		arrow = new GuiComponentSprite(90, 50, iconArrow, texture);
		craftingGrid = new GuiComponentCraftingGrid(25, 30, getFirstRecipeForItem(resultingItem), iconCraftingGrid, texture);

		lblDescription.setScale(0.5f);
		lblDescription.setAdditionalLineHeight(4);
		
		outputSpinner = new GuiComponentItemStackSpinner(150, 40, resultingItem);

		addComponent(lblDescription);
		addComponent(lblTitle);
		addComponent(arrow);
		addComponent(outputSpinner);
		addComponent(craftingGrid);

	}

	@SuppressWarnings("unchecked")
	private static ItemStack[] getFirstRecipeForItem(ItemStack resultingItem) {
		ItemStack[] recipeItems = new ItemStack[9];
		for (IRecipe recipe : (List<IRecipe>)CraftingManager.getInstance().getRecipeList()) {
			if (recipe == null) continue;

			ItemStack result = recipe.getRecipeOutput();
			if (result == null || !result.isItemEqual(resultingItem)) continue;

			Object[] input = getRecipeInput(recipe);
			if (input == null) continue;

			for (int i = 0; i < input.length; i++)
				recipeItems[i] = convertToStack(input[i]);
			break;

		}
		return recipeItems;
	}

	protected static ItemStack convertToStack(Object obj) {
		ItemStack entry = null;
		if (obj instanceof ItemStack) {
			entry = (ItemStack)obj;
		} else if (obj instanceof List) {
			@SuppressWarnings("unchecked")
			List<ItemStack> list = (List<ItemStack>)obj;
			if (list.size() > 0) entry = list.get(0);
		}

		if (entry == null) return null;
		entry = entry.copy();
		if (entry.getItemDamage() == OreDictionary.WILDCARD_VALUE) entry.setItemDamage(0);
		return entry;
	}

	@SuppressWarnings("unchecked")
	private static Object[] getRecipeInput(IRecipe recipe) {
		if (recipe instanceof ShapelessOreRecipe) return ((ShapelessOreRecipe)recipe).getInput().toArray();
		else if (recipe instanceof ShapedOreRecipe) return getShapedOreRecipe((ShapedOreRecipe)recipe);
		else if (recipe instanceof ShapedRecipes) return ((ShapedRecipes)recipe).recipeItems;
		else if (recipe instanceof ShapelessRecipes) return ((ShapelessRecipes)recipe).recipeItems.toArray(new ItemStack[0]);
		return null;
	}

	private static Object[] getShapedOreRecipe(ShapedOreRecipe recipe) {
		try {
			Field field = ShapedOreRecipe.class.getDeclaredField("width");
			if (field != null) {
				field.setAccessible(true);
				int width = field.getInt(recipe);
				Object[] input = recipe.getInput();
				Object[] grid = new Object[9];
				for (int i = 0, offset = 0, y = 0; y < 3; y++) {
					for (int x = 0; x < 3; x++, i++) {
						if (x < width && offset < input.length) {
							grid[i] = input[offset];
							offset++;
						} else {
							grid[i] = null;
						}
					}
				}
				return grid;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int getWidth() {
		return 220;
	}

	@Override
	public int getHeight() {
		return 200;
	}

}
