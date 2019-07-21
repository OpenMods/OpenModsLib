package openmods.fixers;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.IDataFixer;
import net.minecraft.util.datafix.IDataWalker;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class ResourceDataWalker implements IDataWalker {

	private final IForgeRegistryEntry<?> entry;

	private final String idTag;

	public ResourceDataWalker(IForgeRegistryEntry<?> entry, String idTag) {
		this.entry = entry;
		this.idTag = idTag;
	}

	public ResourceDataWalker(IForgeRegistryEntry<?> entry) {
		this(entry, "id");
	}

	@Override
	public CompoundNBT process(IDataFixer fixer, CompoundNBT compound, int version) {
		final ResourceLocation id = new ResourceLocation(compound.getString(idTag));
		final ResourceLocation expected = entry.getRegistryName();
		if (id.equals(expected)) return processImpl(fixer, compound, version);

		return compound;
	}

	protected abstract CompoundNBT processImpl(IDataFixer fixer, CompoundNBT compound, int version);

}
