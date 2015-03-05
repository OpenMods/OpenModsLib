package openmods.utils;

public abstract class LazyValue<T> {

	private boolean initialized;

	private T value;

	protected abstract T initialize();

	public T get() {
		if (!initialized) {
			value = initialize();
			initialized = true;
		}

		return value;
	}

}
