package openmods.utils.bitmap;

import net.minecraft.util.EnumFacing;

public interface IRpcDirectionBitMap {
	void mark(EnumFacing value);

	void clear(EnumFacing value);

	void set(EnumFacing key, boolean value);

	void toggle(EnumFacing value);

	void clearAll();
}
