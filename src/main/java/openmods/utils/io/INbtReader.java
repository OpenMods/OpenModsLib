package openmods.utils.io;

import net.minecraft.nbt.CompoundNBT;

public interface INbtReader<T> {
	T readFromNBT(CompoundNBT tag, String name);
}