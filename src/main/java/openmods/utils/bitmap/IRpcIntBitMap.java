package openmods.utils.bitmap;

public interface IRpcIntBitMap {
	public abstract void mark(Integer value);

	public abstract void clear(Integer value);

	public abstract void toggle(Integer value);

	public abstract void clearAll();
}
