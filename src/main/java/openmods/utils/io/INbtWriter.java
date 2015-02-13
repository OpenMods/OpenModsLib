package openmods.utils.io;

import net.minecraft.nbt.NBTTagCompound;

public interface INbtWriter<T> {
	public void writeToNBT(T o, NBTTagCompound tag, String name);
}