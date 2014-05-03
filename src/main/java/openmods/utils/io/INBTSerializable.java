package openmods.utils.io;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public interface INBTSerializable<T> {
	public T readFromNBT(NBTTagCompound tag, String name);

	public void writeToNBT(T o, NBTTagCompound tag, String name);

	public boolean checkTagType(NBTBase tag);
}