package openmods.fixers;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;

public class NestedItemStackWalker extends ItemTagWalker {

	private final String[] tags;

	public NestedItemStackWalker(IForgeRegistryEntry<Item> entry, String... tags) {
		super(entry);
		this.tags = tags;
	}

	@Override
	protected NBTTagCompound processTag(IDataFixer fixer, NBTTagCompound compound, int version) {
		for (String tag : tags)
			compound = DataFixesManager.processItemStack(fixer, compound, version, tag);

		return compound;
	}

}
