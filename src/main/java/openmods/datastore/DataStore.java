package openmods.datastore;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class DataStore<K, V> {
	private final Map<K, V> values;

	DataStore(Map<K, V> values) {
		this.values = ImmutableMap.copyOf(values);
	}

	public V get(K key) {
		return values.get(key);
	}

	void visit(IDataVisitor<K, V> visitor) {
		visitor.begin(values.size());
		for (Map.Entry<K, V> e : values.entrySet())
			visitor.entry(e.getKey(), e.getValue());

		visitor.end();
	}
}
