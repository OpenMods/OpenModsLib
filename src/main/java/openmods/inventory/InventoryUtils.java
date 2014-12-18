package openmods.inventory;

import java.util.*;

import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.inventory.StackEqualityTesterBuilder.IEqualityTester;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class InventoryUtils {

	public static final IEqualityTester STACK_EQUALITY_TESTER = new StackEqualityTesterBuilder()
			.useItem()
			.useDamage()
			.useNBT()
			.build();

	private static IInventory doubleChestFix(TileEntity te) {
		final World world = te.getWorldObj();
		final int x = te.xCoord;
		final int y = te.yCoord;
		final int z = te.zCoord;
		if (world.getBlock(x - 1, y, z) == Blocks.chest) return new InventoryLargeChest("Large chest", (IInventory)world.getTileEntity(x - 1, y, z), (IInventory)te);
		if (world.getBlock(x + 1, y, z) == Blocks.chest) return new InventoryLargeChest("Large chest", (IInventory)te, (IInventory)world.getTileEntity(x + 1, y, z));
		if (world.getBlock(x, y, z - 1) == Blocks.chest) return new InventoryLargeChest("Large chest", (IInventory)world.getTileEntity(x, y, z - 1), (IInventory)te);
		if (world.getBlock(x, y, z + 1) == Blocks.chest) return new InventoryLargeChest("Large chest", (IInventory)te, (IInventory)world.getTileEntity(x, y, z + 1));
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
	 * Get the indexes of all slots containing a stack of the supplied item
	 * type.
	 *
	 * @param inventory
	 * @param stack
	 * @return Returns a set of the slot indexes
	 */
	public static Set<Integer> getSlotsWithStack(IInventory inventory, ItemStack stack) {
		inventory = getInventory(inventory);
		Set<Integer> slots = Sets.newHashSet();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stackInSlot = inventory.getStackInSlot(i);
			if (stackInSlot != null && stackInSlot.isItemEqual(stack)) slots.add(i);
		}
		return slots;
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
	 * Consume ONE of the supplied item types
	 *
	 * @param inventory
	 * @param stack
	 * @return Returns whether or not it was able to
	 */
	public static boolean consumeInventoryItem(IInventory inventory, ItemStack stack) {
		int slotWithStack = getFirstSlotWithStack(inventory, stack);
		if (slotWithStack > -1) {
			ItemStack stackInSlot = inventory.getStackInSlot(slotWithStack);
			stackInSlot.stackSize--;
			if (stackInSlot.stackSize == 0) {
				inventory.setInventorySlotContents(slotWithStack, null);
			}
			return true;
		}
		return false;
	}

	/**
	 * Get the first slot index in an inventory with an item
	 *
	 * @param invent
	 * @return The slot index, or -1 if the inventory is empty
	 */
	public static int getSlotIndexOfNextStack(IInventory invent) {
		for (int i = 0; i < invent.getSizeInventory(); i++) {
			ItemStack stack = invent.getStackInSlot(i);
			if (stack != null) { return i; }
		}
		return -1;
	}

	/***
	 * Removes an item stack from the inventory and returns a copy of it
	 *
	 * @param invent
	 * @return A copy of the stack it removed
	 */
	public static ItemStack removeNextItemStack(IInventory invent) {
		int nextFilledSlot = getSlotIndexOfNextStack(invent);
		if (nextFilledSlot > -1) {
			ItemStack copy = invent.getStackInSlot(nextFilledSlot).copy();
			invent.setInventorySlotContents(nextFilledSlot, null);
			return copy;
		}
		return null;
	}

	public static Set<Integer> getAllSlots(IInventory inventory) {
		inventory = getInventory(inventory);
		Set<Integer> slots = new HashSet<Integer>();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			slots.add(i);
		}
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
	
	public static int addItems(ItemStack stack, int amount) {
		final int newSize = stack.stackSize + amount;
		final int maxSize = stack.getMaxStackSize();
		if (newSize > maxSize) {
			stack.stackSize = maxSize;
			return newSize - maxSize;
		}
		
		stack.stackSize = newSize;
		return 0;
	}

	public static ItemStack copyItem(ItemStack stack) {
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
		inventory.markDirty();
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
		inventory.markDirty();
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
