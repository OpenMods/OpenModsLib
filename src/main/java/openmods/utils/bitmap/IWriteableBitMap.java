package openmods.utils.bitmap;

public interface IWriteableBitMap<T> {

	void mark(T value);

	void clear(T value);

	void set(T key, boolean value);

	void toggle(T value);

	void clearAll();

}