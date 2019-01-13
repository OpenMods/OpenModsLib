package openmods.utils.io;

import net.minecraft.nbt.NBTBase;

public interface INbtChecker {
	boolean checkTagType(NBTBase tag);
}