package openmods.fixers;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.IForgeRegistryEntry;
import openmods.inventory.GenericInventory;
import openmods.inventory.ItemInventory;

public class ItemInventoryWalker extends ItemTagWalker {

	public ItemInventoryWalker(IForgeRegistryEntry<?> entry) {
		super(entry);
	}

	@Override
	protected CompoundNBT processTag(IDataFixer fixer, CompoundNBT compound, int version) {
		if (compound.hasKey(ItemInventory.TAG_INVENTORY, Constants.NBT.TAG_COMPOUND)) {
			final CompoundNBT inventoryTag = compound.getCompoundTag(ItemInventory.TAG_INVENTORY);
			final CompoundNBT newInventoryTag = DataFixesManager.processInventory(fixer, inventoryTag, version, GenericInventory.TAG_ITEMS);
			compound.setTag(ItemInventory.TAG_INVENTORY, newInventoryTag);
		}

		return compound;
	}
}
