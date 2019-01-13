package openmods.api;

@FunctionalInterface
public interface IValueReceiver<T> {
	void setValue(T value);
}
