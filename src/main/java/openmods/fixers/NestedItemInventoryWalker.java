package openmods.fixers;

import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class NestedItemInventoryWalker extends ItemTagWalker {

	private final String[] tags;

	public NestedItemInventoryWalker(IForgeRegistryEntry<Item> entry, String... tags) {
		super(entry);
		this.tags = tags;
	}

	@Override
	protected CompoundNBT processTag(IDataFixer fixer, CompoundNBT compound, int version) {
		for (String tag : tags)
			compound = DataFixesManager.processInventory(fixer, compound, version, tag);

		return compound;
	}

}
