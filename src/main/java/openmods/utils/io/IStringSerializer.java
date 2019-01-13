package openmods.utils.io;

public interface IStringSerializer<T> {
	T readFromString(String s);
	// toString not needed!
}
