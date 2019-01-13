package openmods.api;

@FunctionalInterface
public interface IValueProvider<T> {
	T getValue();
}
