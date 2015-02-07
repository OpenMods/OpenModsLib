package openmods.utils;

import com.google.common.base.Throwables;

public class CachedInstanceFactory<V> extends CachedFactory<Class<? extends V>, V> {

	@Override
	protected V create(Class<? extends V> key) {
		try {
			return key.newInstance();
		} catch (Throwable t) {
			throw Throwables.propagate(t);
		}
	}

	public static <V> CachedInstanceFactory<V> create() {
		return new CachedInstanceFactory<V>();
	}
}
