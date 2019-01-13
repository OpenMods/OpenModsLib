package openmods.utils.io;

import net.minecraft.nbt.NBTTagCompound;

public interface INbtReader<T> {
	T readFromNBT(NBTTagCompound tag, String name);
}