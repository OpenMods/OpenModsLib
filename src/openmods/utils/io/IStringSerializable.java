package openmods.utils.io;

public interface IStringSerializable<T> {
	public T readFromString(String s);
	// toString not needed!
}
