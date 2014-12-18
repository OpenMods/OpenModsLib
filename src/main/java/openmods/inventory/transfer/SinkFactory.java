package openmods.inventory.transfer;

import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.integration.modules.BuildCraftPipes;
import openmods.integration.modules.BuildCraftSink;
import openmods.inventory.InventoryUtils;
import openmods.inventory.transfer.sinks.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class SinkFactory {

	private static IItemStackSink createSinkForSlot(IInventory inventory, int slot) {
		return new SingleInventorySlotSink(inventory, slot) {
			@Override
			protected void markDirty() {}
		};
	}

	private static IItemStackSink createSinkForSlot(final ISidedInventory inventory, ForgeDirection side, int slot) {
		return new SingleSidedSlotSink(inventory, slot, side) {
			@Override
			protected void markDirty() {}
		};
	}

	private static IItemStackSink createSinkForInventory(final IInventory inventory, List<IItemStackSink> slots) {
		return new MultipleSlotSink(slots) {
			@Override
			protected void markDirty() {
				inventory.markDirty();
			}
		};
	}

	public static IItemStackSink wrap(IInventory inventory) {
		final IInventory usedInventory = InventoryUtils.getInventory(inventory);
		List<IItemStackSink> slots = Lists.newArrayList();

		for (int i = 0; i < usedInventory.getSizeInventory(); i++)
			slots.add(createSinkForSlot(usedInventory, i));

		return createSinkForInventory(usedInventory, slots);
	}

	public static IItemStackSink wrap(IInventory inventory, int... slots) {
		final IInventory usedInventory = InventoryUtils.getInventory(inventory);
		List<IItemStackSink> sinks = Lists.newArrayList();

		for (int slot : slots)
			sinks.add(createSinkForSlot(usedInventory, slot));

		return createSinkForInventory(usedInventory, sinks);
	}

	public static IItemStackSink wrap(IInventory inventory, Iterable<Integer> slots) {
		final IInventory usedInventory = InventoryUtils.getInventory(inventory);
		List<IItemStackSink> sinks = Lists.newArrayList();

		for (int slot : slots)
			sinks.add(createSinkForSlot(usedInventory, slot));

		return createSinkForInventory(usedInventory, sinks);
	}

	public static IItemStackSink wrap(final ISidedInventory inventory, ForgeDirection side, int... slots) {
		List<IItemStackSink> sinks = Lists.newArrayList();

		for (int slot : slots)
			sinks.add(createSinkForSlot(inventory, side, slot));

		return createSinkForInventory(inventory, sinks);
	}

	public static IItemStackSink wrap(final ISidedInventory inventory, ForgeDirection side, Iterable<Integer> slots) {
		List<IItemStackSink> sinks = Lists.newArrayList();

		for (int slot : slots)
			sinks.add(createSinkForSlot(inventory, side, slot));

		return createSinkForInventory(inventory, sinks);
	}

	public static IItemStackSink wrap(final ISidedInventory inventory, ForgeDirection side) {
		List<IItemStackSink> slots = Lists.newArrayList();

		for (int slot : inventory.getAccessibleSlotsFromSide(side.ordinal()))
			slots.add(createSinkForSlot(inventory, side, slot));

		return createSinkForInventory(inventory, slots);
	}

	public static IItemStackSink wrapSlots(Iterable<IItemStackSink> sinks) {
		return new MultipleSlotSink(sinks) {
			@Override
			protected void markDirty() {}
		};
	}

	public static IItemStackSink wrapSlots(IItemStackSink... sinks) {
		return new MultipleSlotSink(ImmutableList.copyOf(sinks)) {
			@Override
			protected void markDirty() {}
		};
	}

	public IItemStackSink wrapObject(Object target, ForgeDirection side) {
		if (target instanceof ISidedInventory) return wrap((ISidedInventory)target, side);
		if (target instanceof IInventory) return wrap((IInventory)target);
		return null;
	}

	public IItemStackSink wrapTileEntity(TileEntity te, ForgeDirection side) {
		if (BuildCraftPipes.access().isPipe(te)) return new BuildCraftSink(te, side);
		return wrapObject(te, side);
	}

}
