package openmods.utils;

import java.util.Map;

import com.google.common.collect.Maps;

public abstract class CachedFactory<K, V> {

	private final Map<K, V> cache = Maps.newHashMap();

	public V getOrCreate(K key) {
		V value = cache.get(key);

		if (value == null) {
			value = create(key);
			cache.put(key, value);
		}

		return value;
	}

	public V remove(K key) {
		return cache.remove(key);
	}

	protected abstract V create(K key);
}
