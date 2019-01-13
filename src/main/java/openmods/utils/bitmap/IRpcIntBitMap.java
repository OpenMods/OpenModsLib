package openmods.utils.bitmap;

public interface IRpcIntBitMap {
	void mark(Integer value);

	void clear(Integer value);

	void set(Integer key, boolean value);

	void toggle(Integer value);

	void clearAll();
}
