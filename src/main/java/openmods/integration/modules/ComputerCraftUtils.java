package openmods.integration.modules;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import openmods.Mods;

public final class ComputerCraftUtils {

	private static final Container DUMMY_CONTAINER = new Container() {

		@Override
		public boolean canInteractWith(EntityPlayer playerIn) {
			return false;
		}

	};

	@ObjectHolder(Mods.COMPUTERCRAFT)
	private static class Turtles {

		@ObjectHolder("CC-TurtleExpanded")
		public static Item normalTurtle;

		@ObjectHolder("CC-TurtleAdvanced")
		public static Item advancedTurtle;

	}

	public static Object[] wrap(Object... args) {
		return args;
	}

	protected static IRecipe findTurtleUpgradeRecipe() {
		for (IRecipe recipe : CraftingManager.getInstance().getRecipeList())
			if (recipe.getClass().getSimpleName().equals("TurtleUpgradeRecipe")) return recipe;

		return null;
	}

	public static void addUpgradedTurtles(List<ItemStack> result, ItemStack upgradeItem) {
		IRecipe recipe = findTurtleUpgradeRecipe();
		if (recipe != null) addTurtlesForUpgrade(result, recipe, upgradeItem);
	}

	private static void addTurtlesForUpgrade(List<ItemStack> result, IRecipe recipe, ItemStack upgrade) {
		if (Turtles.normalTurtle != null) addTurtlesForUpgrade(result, recipe, Turtles.normalTurtle, upgrade);
		if (Turtles.advancedTurtle != null) addTurtlesForUpgrade(result, recipe, Turtles.advancedTurtle, upgrade);
	}

	private static void addTurtlesForUpgrade(List<ItemStack> result, IRecipe recipe, Item turtle, ItemStack upgrade) {
		final ItemStack turtleStack = new ItemStack(turtle);
		addTurtleForUpgrade(result, recipe, turtleStack, null, upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Blocks.CRAFTING_TABLE), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_SWORD), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_SHOVEL), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_PICKAXE), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_AXE), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_HOE), upgrade);
	}

	private static void addTurtleForUpgrade(List<ItemStack> result, IRecipe recipe, ItemStack turtle, ItemStack left, ItemStack right) {
		final InventoryCrafting inv = new InventoryCrafting(DUMMY_CONTAINER, 3, 3);
		inv.setInventorySlotContents(0, left);
		inv.setInventorySlotContents(1, turtle);
		inv.setInventorySlotContents(2, right);

		final ItemStack upgradedTurtle = recipe.getCraftingResult(inv);
		if (upgradedTurtle != null) result.add(upgradedTurtle);
	}

}