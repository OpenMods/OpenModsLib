package openmods.utils.io;

import net.minecraft.nbt.CompoundNBT;

public interface INbtWriter<T> {
	void writeToNBT(T o, CompoundNBT tag, String name);
}