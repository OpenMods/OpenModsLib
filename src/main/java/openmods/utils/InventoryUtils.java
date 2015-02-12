package openmods.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class InventoryUtils {

	public static boolean areItemAndTagEqual(final ItemStack stackA, ItemStack stackB) {
		return stackA.isItemEqual(stackB) && ItemStack.areItemStackTagsEqual(stackA, stackB);
	}

	public static boolean areMergeCandidates(ItemStack source, ItemStack target) {
		return areItemAndTagEqual(source, target) && target.stackSize < target.getMaxStackSize();
	}

	public static ItemStack copyAndChange(ItemStack stack, int newSize) {
		ItemStack copy = stack.copy();
		copy.stackSize = newSize;
		return copy;
	}

	public static void removeFromSlot(IInventory inventory, int slot, int amount) {
		ItemStack sourceStack = inventory.getStackInSlot(slot);
		sourceStack.stackSize -= amount;
		if (sourceStack.stackSize == 0) {
			inventory.setInventorySlotContents(slot, null);
		} else {
			// Paranoia? Always!
			inventory.setInventorySlotContents(slot, sourceStack);
		}
	}

	private static IInventory doubleChestFix(TileEntity te) {
		final World world = te.getWorldObj();
		final int x = te.xCoord;
		final int y = te.yCoord;
		final int z = te.zCoord;
		final Block block = te.getBlockType();
		if (world.getBlock(x - 1, y, z) == block) return new InventoryLargeChest("Large chest", (IInventory)world.getTileEntity(x - 1, y, z), (IInventory)te);
		if (world.getBlock(x + 1, y, z) == block) return new InventoryLargeChest("Large chest", (IInventory)te, (IInventory)world.getTileEntity(x + 1, y, z));
		if (world.getBlock(x, y, z - 1) == block) return new InventoryLargeChest("Large chest", (IInventory)world.getTileEntity(x, y, z - 1), (IInventory)te);
		if (world.getBlock(x, y, z + 1) == block) return new InventoryLargeChest("Large chest", (IInventory)te, (IInventory)world.getTileEntity(x, y, z + 1));
		return (te instanceof IInventory)? (IInventory)te : null;
	}

	public static IInventory getInventory(World world, int x, int y, int z) {
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof TileEntityChest) return doubleChestFix(tileEntity);
		if (tileEntity instanceof IInventory) return (IInventory)tileEntity;
		return null;
	}

	public static IInventory getInventory(World world, int x, int y, int z, ForgeDirection direction) {
		if (direction != null) {
			x += direction.offsetX;
			y += direction.offsetY;
			z += direction.offsetZ;
		}
		return getInventory(world, x, y, z);

	}

	public static IInventory getInventory(IInventory inventory) {
		if (inventory instanceof TileEntityChest) return doubleChestFix((TileEntity)inventory);
		return inventory;
	}

	public static List<ItemStack> getInventoryContents(IInventory inventory) {
		List<ItemStack> result = Lists.newArrayList();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack slot = inventory.getStackInSlot(i);
			if (slot != null) result.add(slot);
		}
		return result;
	}

	/***
	 * Get the first slot containing an item type matching the supplied type.
	 *
	 * @param inventory
	 * @param stack
	 * @return Returns -1 if none found
	 */
	public static int getFirstSlotWithStack(IInventory inventory, ItemStack stack) {
		inventory = getInventory(inventory);
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stackInSlot = inventory.getStackInSlot(i);
			if (stackInSlot != null && stackInSlot.isItemEqual(stack)) { return i; }
		}
		return -1;
	}

	/***
	 * Get the indexes of all slots containing a stack of the supplied item
	 * type.
	 *
	 * @param inventory
	 * @param stack
	 * @return Returns a set of the slot indexes
	 */
	public static Set<Integer> getAllSlotsWithStack(IInventory inventory, ItemStack stack) {
		inventory = getInventory(inventory);
		Set<Integer> slots = Sets.newHashSet();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stackInSlot = inventory.getStackInSlot(i);
			if (stackInSlot != null && stackInSlot.isItemEqual(stack)) slots.add(i);
		}
		return slots;
	}

	public static int getFirstNonEmptySlot(IInventory inventory) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack != null) { return i; }
		}
		return -1;
	}

	public static Set<Integer> getAllSlots(IInventory inventory) {
		inventory = getInventory(inventory);
		Set<Integer> slots = Sets.newHashSet();
		for (int i = 0; i < inventory.getSizeInventory(); i++)
			slots.add(i);

		return slots;
	}

	public static Map<Integer, ItemStack> getAllItems(IInventory inventory) {
		Map<Integer, ItemStack> result = Maps.newHashMap();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack != null) result.put(i, stack);
		}

		return result;

	}

	public static boolean inventoryIsEmpty(IInventory inventory) {
		for (int i = 0, l = inventory.getSizeInventory(); i < l; i++)
			if (inventory.getStackInSlot(i) != null) return false;
		return true;
	}

	public static boolean tryMergeStacks(ItemStack stackToMerge, ItemStack stackInSlot) {
		if (stackInSlot == null || !stackInSlot.isItemEqual(stackToMerge) || !ItemStack.areItemStackTagsEqual(stackToMerge, stackInSlot)) return false;

		int newStackSize = stackInSlot.stackSize + stackToMerge.stackSize;

		final int maxStackSize = stackToMerge.getMaxStackSize();
		if (newStackSize <= maxStackSize) {
			stackToMerge.stackSize = 0;
			stackInSlot.stackSize = newStackSize;
			return true;
		} else if (stackInSlot.stackSize < maxStackSize) {
			stackToMerge.stackSize -= maxStackSize - stackInSlot.stackSize;
			stackInSlot.stackSize = maxStackSize;
			return true;
		}

		return false;
	}

	public static ItemStack returnItem(ItemStack stack) {
		return (stack == null || stack.stackSize <= 0)? null : stack.copy();
	}

	public static void swapStacks(IInventory inventory, int slot1, int slot2) {
		swapStacks(inventory, slot1, slot2, true, true);
	}

	public static void swapStacks(IInventory inventory, int slot1, int slot2, boolean copy, boolean validate) {
		Preconditions.checkElementIndex(slot1, inventory.getSizeInventory(), "input slot id");
		Preconditions.checkElementIndex(slot2, inventory.getSizeInventory(), "output slot id");

		ItemStack stack1 = inventory.getStackInSlot(slot1);
		ItemStack stack2 = inventory.getStackInSlot(slot2);

		if (validate) {
			isItemValid(inventory, slot2, stack1);
			isItemValid(inventory, slot1, stack2);
		}

		if (copy) {
			if (stack1 != null) stack1 = stack1.copy();
			if (stack2 != null) stack2 = stack2.copy();
		}

		inventory.setInventorySlotContents(slot1, stack2);
		inventory.setInventorySlotContents(slot2, stack1);
	}

	public static void swapStacks(ISidedInventory inventory, int slot1, ForgeDirection side1, int slot2, ForgeDirection side2) {
		swapStacks(inventory, slot1, side1, slot2, side2, true, true);
	}

	public static void swapStacks(ISidedInventory inventory, int slot1, ForgeDirection side1, int slot2, ForgeDirection side2, boolean copy, boolean validate) {
		Preconditions.checkElementIndex(slot1, inventory.getSizeInventory(), "input slot id");
		Preconditions.checkElementIndex(slot2, inventory.getSizeInventory(), "output slot id");

		ItemStack stack1 = inventory.getStackInSlot(slot1);
		ItemStack stack2 = inventory.getStackInSlot(slot2);

		if (validate) {
			isItemValid(inventory, slot2, stack1);
			isItemValid(inventory, slot1, stack2);

			canExtract(inventory, slot1, side1, stack1);
			canInsert(inventory, slot2, side2, stack1);

			canExtract(inventory, slot2, side2, stack2);
			canInsert(inventory, slot1, side1, stack2);
		}

		if (copy) {
			if (stack1 != null) stack1 = stack1.copy();
			if (stack2 != null) stack2 = stack2.copy();
		}

		inventory.setInventorySlotContents(slot1, stack2);
		inventory.setInventorySlotContents(slot2, stack1);
	}

	protected static void isItemValid(IInventory inventory, int slot, ItemStack stack) {
		Preconditions.checkArgument(inventory.isItemValidForSlot(slot, stack), "Slot %s cannot accept item", slot);
	}

	protected static void canInsert(ISidedInventory inventory, int slot, ForgeDirection side, ItemStack stack) {
		Preconditions.checkArgument(inventory.canInsertItem(slot, stack, side.ordinal()),
				"Item cannot be inserted into slot %s on side %s", slot, side);
	}

	protected static void canExtract(ISidedInventory inventory, int slot, ForgeDirection side, ItemStack stack) {
		Preconditions.checkArgument(inventory.canExtractItem(slot, stack, side.ordinal()),
				"Item cannot be extracted from slot %s on side %s", slot, side);
	}

}
