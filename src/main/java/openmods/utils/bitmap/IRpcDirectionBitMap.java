package openmods.utils.bitmap;

import net.minecraft.util.EnumFacing;

public interface IRpcDirectionBitMap {
	public void mark(EnumFacing value);

	public void clear(EnumFacing value);

	public abstract void set(EnumFacing key, boolean value);

	public void toggle(EnumFacing value);

	public abstract void clearAll();
}
