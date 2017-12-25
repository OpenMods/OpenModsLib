package openmods.api;

@FunctionalInterface
public interface IValueProvider<T> {
	public T getValue();
}
