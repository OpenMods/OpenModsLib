package openmods.utils.io;

import net.minecraft.nbt.INBT;

public interface INbtChecker {
	boolean checkTagType(INBT tag);
}