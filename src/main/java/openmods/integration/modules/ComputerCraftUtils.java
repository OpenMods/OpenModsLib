package openmods.integration.modules;

import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.registries.ObjectHolder;
import openmods.Mods;

public final class ComputerCraftUtils {

	private static final Container DUMMY_CONTAINER = new Container(null, -1) {

		@Override
		public boolean canInteractWith(PlayerEntity playerIn) {
			return false;
		}

	};

	@ObjectHolder(Mods.COMPUTERCRAFT)
	private static class Turtles {

		@ObjectHolder("turtle_expanded")
		public static Item normalTurtle;

		@ObjectHolder("turtle_advanced")
		public static Item advancedTurtle;

	}

	public static Object[] wrap(Object... args) {
		return args;
	}

	protected static IRecipe findTurtleUpgradeRecipe() {
		// TODO 1.14 Old method requires world, re-do?
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
		addTurtleForUpgrade(result, recipe, turtleStack, ItemStack.EMPTY, upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Blocks.CRAFTING_TABLE), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_SWORD), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_SHOVEL), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_PICKAXE), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_AXE), upgrade);
		addTurtleForUpgrade(result, recipe, turtleStack, new ItemStack(Items.DIAMOND_HOE), upgrade);
	}

	private static void addTurtleForUpgrade(List<ItemStack> result, IRecipe recipe, ItemStack turtle, ItemStack left, ItemStack right) {
		final CraftingInventory inv = new CraftingInventory(DUMMY_CONTAINER, 3, 3);
		inv.setInventorySlotContents(0, left);
		inv.setInventorySlotContents(1, turtle);
		inv.setInventorySlotContents(2, right);

		final ItemStack upgradedTurtle = recipe.getCraftingResult(inv);
		if (!upgradedTurtle.isEmpty()) result.add(upgradedTurtle);
	}

}