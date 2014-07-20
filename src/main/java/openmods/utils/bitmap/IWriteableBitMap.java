package openmods.utils.bitmap;

public interface IWriteableBitMap<T> {

	public abstract void mark(T value);

	public abstract void clear(T value);

	public abstract void toggle(T value);

	public abstract void clearAll();

}