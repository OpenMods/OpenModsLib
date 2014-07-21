package openmods.utils.bitmap;

import net.minecraftforge.common.util.ForgeDirection;

public interface IRpcDirectionBitMap {
	public void mark(ForgeDirection value);

	public void clear(ForgeDirection value);

	public abstract void set(ForgeDirection key, boolean value);

	public void toggle(ForgeDirection value);

	public abstract void clearAll();
}
