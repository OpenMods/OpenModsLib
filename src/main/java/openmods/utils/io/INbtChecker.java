package openmods.utils.io;

import net.minecraft.nbt.NBTBase;

public interface INbtChecker {
	public boolean checkTagType(NBTBase tag);
}