package openmods.utils.bitmap;

public interface IWriteableBitMap<T> {

	public void mark(T value);

	public void clear(T value);

	public void set(T key, boolean value);

	public void toggle(T value);

	public void clearAll();

}