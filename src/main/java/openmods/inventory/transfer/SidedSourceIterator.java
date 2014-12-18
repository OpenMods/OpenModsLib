package openmods.inventory.transfer;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.inventory.transfer.sources.IItemStackSource;
import openmods.inventory.transfer.sources.SidedInventorySource;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

public class SidedSourceIterator extends InventorySourceIterator {

	private final ForgeDirection side;

	public SidedSourceIterator(Predicate<ItemStack> predicate, ForgeDirection direction, ISidedInventory inventory, int... slots) {
		super(predicate, inventory, slots);
		this.side = direction;
	}

	public SidedSourceIterator(Predicate<ItemStack> predicate, ForgeDirection direction, ISidedInventory inventory) {
		super(predicate, inventory, Iterators.forArray(ArrayUtils.toObject(inventory.getAccessibleSlotsFromSide(direction.ordinal()))));
		this.side = direction;
	}

	@Override
	protected IItemStackSource createSource(IInventory inventory, int slot) {
		return new SidedInventorySource((ISidedInventory)inventory, slot, side);
	}

}
