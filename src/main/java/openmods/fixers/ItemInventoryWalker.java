package openmods.fixers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import openmods.inventory.GenericInventory;
import openmods.inventory.ItemInventory;

public class ItemInventoryWalker extends ItemTagWalker {

	public ItemInventoryWalker(IForgeRegistryEntry<?> entry) {
		super(entry);
	}

	@Override
	protected NBTTagCompound processTag(IDataFixer fixer, NBTTagCompound compound, int version) {
		if (compound.hasKey(ItemInventory.TAG_INVENTORY, Constants.NBT.TAG_COMPOUND)) {
			final NBTTagCompound inventoryTag = compound.getCompoundTag(ItemInventory.TAG_INVENTORY);
			final NBTTagCompound newInventoryTag = DataFixesManager.processInventory(fixer, inventoryTag, version, GenericInventory.TAG_ITEMS);
			compound.setTag(ItemInventory.TAG_INVENTORY, newInventoryTag);
		}

		return compound;
	}
}
