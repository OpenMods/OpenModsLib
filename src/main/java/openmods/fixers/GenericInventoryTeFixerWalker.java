package openmods.fixers;

import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;
import openmods.inventory.GenericInventory;

public class GenericInventoryTeFixerWalker implements IFixerFactory {

	@Override
	public void register(DataFixer registry, Class<?> registeringClass) {
		registry.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists(registeringClass, GenericInventory.TAG_ITEMS));
	}

}
