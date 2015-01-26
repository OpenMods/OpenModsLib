package openmods.utils.io;

import net.minecraft.nbt.NBTBase;

public interface INbtChecker<T> {

	public abstract boolean checkTagType(NBTBase tag);

}