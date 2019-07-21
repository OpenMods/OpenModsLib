package openmods.utils.bitmap;

import net.minecraft.util.Direction;

public interface IRpcDirectionBitMap {
	void mark(Direction value);

	void clear(Direction value);

	void set(Direction key, boolean value);

	void toggle(Direction value);

	void clearAll();
}
