package openmods.utils;

public class CachedInstanceFactory<V> extends CachedFactory<Class<? extends V>, V> {

	@Override
	protected V create(Class<? extends V> key) {
		try {
			return key.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static <V> CachedInstanceFactory<V> create() {
		return new CachedInstanceFactory<>();
	}
}
