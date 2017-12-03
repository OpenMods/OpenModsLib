package openmods.fixers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;

public abstract class ItemTagWalker extends ResourceDataWalker {

	public ItemTagWalker(IForgeRegistryEntry<?> entry) {
		super(entry);
	}

	@Override
	protected final NBTTagCompound processImpl(IDataFixer fixer, NBTTagCompound compound, int version) {
		if (compound.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
			final NBTTagCompound tag = compound.getCompoundTag("tag");
			final NBTTagCompound newTag = processTag(fixer, tag, version);
			compound.setTag("tag", newTag);
		}

		return compound;
	}

	protected abstract NBTTagCompound processTag(IDataFixer fixer, NBTTagCompound tag, int version);
}
