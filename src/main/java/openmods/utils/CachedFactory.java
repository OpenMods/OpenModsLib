package openmods.utils;

import com.google.common.collect.Maps;
import java.util.Map;

@Deprecated
public abstract class CachedFactory<K, V> {

	private final Map<K, V> cache = Maps.newHashMap();

	public V getOrCreate(K key) {
		return cache.computeIfAbsent(key, this::create);
	}

	public V remove(K key) {
		return cache.remove(key);
	}

	protected abstract V create(K key);
}
