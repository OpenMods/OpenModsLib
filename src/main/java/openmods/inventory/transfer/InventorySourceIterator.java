package openmods.inventory.transfer;

import java.util.Iterator;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import openmods.inventory.transfer.sources.IItemStackSource;
import openmods.inventory.transfer.sources.InventorySlotSource;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

public class InventorySourceIterator extends AbstractIterator<IItemStackSource> {

	private final Predicate<ItemStack> predicate;

	private final IInventory inventory;

	private final Iterator<Integer> slots;

	private static Iterator<Integer> createRange(int from, int to) {
		Integer[] result = new Integer[to - from];
		for (int i = from; i < to; i++)
			result[i] = i;

		return Iterators.forArray(result);
	}

	protected InventorySourceIterator(Predicate<ItemStack> predicate, IInventory inventory, Iterator<Integer> slots) {
		this.predicate = predicate;
		this.inventory = inventory;
		this.slots = slots;
	}

	public InventorySourceIterator(Predicate<ItemStack> predicate, IInventory inventory, int... slots) {
		this(predicate, inventory, Iterators.forArray(ArrayUtils.toObject(slots)));
	}

	public InventorySourceIterator(Predicate<ItemStack> predicate, IInventory inventory) {
		this(predicate, inventory, createRange(0, inventory.getSizeInventory()));
	}

	protected IItemStackSource createSource(IInventory inventory, int slot) {
		return new InventorySlotSource(inventory, slot);
	}

	@Override
	protected IItemStackSource computeNext() {
		while (slots.hasNext()) {
			int slot = slots.next();
			ItemStack inSlot = inventory.getStackInSlot(slot);
			if (predicate.apply(inSlot)) return createSource(inventory, slot);
		}

		return endOfData();
	}
}
