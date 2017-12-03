package openmods.fixers;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;

public class NestedItemInventoryWalker extends ItemTagWalker {

	private final String[] tags;

	public NestedItemInventoryWalker(IForgeRegistryEntry<Item> entry, String... tags) {
		super(entry);
		this.tags = tags;
	}

	@Override
	protected NBTTagCompound processTag(IDataFixer fixer, NBTTagCompound compound, int version) {
		for (String tag : tags)
			compound = DataFixesManager.processInventory(fixer, compound, version, tag);

		return compound;
	}

}
