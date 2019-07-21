package openmods.fixers;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class ItemTagWalker extends ResourceDataWalker {

	public ItemTagWalker(IForgeRegistryEntry<?> entry) {
		super(entry);
	}

	@Override
	protected final CompoundNBT processImpl(IDataFixer fixer, CompoundNBT compound, int version) {
		if (compound.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
			final CompoundNBT tag = compound.getCompoundTag("tag");
			final CompoundNBT newTag = processTag(fixer, tag, version);
			compound.setTag("tag", newTag);
		}

		return compound;
	}

	protected abstract CompoundNBT processTag(IDataFixer fixer, CompoundNBT tag, int version);
}
