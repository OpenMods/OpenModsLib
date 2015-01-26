package openmods.utils.io;

public interface IStringSerializer<T> {
	public T readFromString(String s);
	// toString not needed!
}
