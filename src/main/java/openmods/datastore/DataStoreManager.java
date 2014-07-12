package openmods.datastore;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;

public class DataStoreManager {

	public static class UnknownKey extends RuntimeException {
		private static final long serialVersionUID = -1956682447443082464L;

		public UnknownKey(DataStoreKey<?, ?> key) {
			super(key.toString());
		}
	}

	public static class UnknownKeyId extends RuntimeException {
		private static final long serialVersionUID = -997878954285130254L;

		public UnknownKeyId(String id) {
			super(id);
		}
	}

	private final BiMap<DataStoreKey<?, ?>, String> dataStoreKeys = HashBiMap.create();

	protected final Map<DataStoreKey<?, ?>, DataStoreWrapper<?, ?>> dataStoreMeta = Maps.newHashMap();

	private <K, V> void checkKeyExists(DataStoreKey<K, V> key) {
		if (!dataStoreKeys.containsKey(key)) throw new UnknownKey(key);
	}

	protected <K, V> DataStoreWrapper<K, V> getDataStoreMeta(DataStoreKey<K, V> key) {
		checkKeyExists(key);

		@SuppressWarnings("unchecked")
		DataStoreWrapper<K, V> meta = (DataStoreWrapper<K, V>)dataStoreMeta.get(key);
		if (meta == null) throw new UnknownKey(key);
		return meta;
	}

	protected <V, K> DataStoreWrapper<K, V> getDataStoreMeta(String id) {
		@SuppressWarnings("unchecked")
		final DataStoreKey<K, V> key = (DataStoreKey<K, V>)dataStoreKeys.inverse().get(id);
		if (key == null) throw new UnknownKeyId(id);

		return getDataStoreMeta(key);
	}

	public <K, V> DataStore<K, V> getLocalDataStore(DataStoreKey<K, V> key) {
		return getDataStoreMeta(key).localData();
	}

	public <K, V> DataStore<K, V> getActiveDataStore(DataStoreKey<K, V> key) {
		return getDataStoreMeta(key).activeData();
	}

	public <K, V> DataStoreReader<K, V> createDataStoreReader(String id) {
		return this.<V, K> getDataStoreMeta(id).createReader();
	}

	public <K, V> DataStoreWriter<K, V> createDataStoreWriter(DataStoreKey<K, V> key) {
		return getDataStoreMeta(key).createWriter();
	}

	public <K, V> void addCallback(DataStoreKey<K, V> key, IDataVisitor<K, V> visitor) {
		getDataStoreMeta(key).addVisitor(visitor);
	}

	public void activateLocalData() {
		for (DataStoreWrapper<?, ?> meta : dataStoreMeta.values())
			meta.activateLocalData();
	}

	public <K, V> DataStoreBuilder<K, V> createDataStore(String id, Class<? extends K> keyClass, Class<? extends V> valueClass) {
		DataStoreKey<K, V> key = new DataStoreKey<K, V>(id);
		String prev = dataStoreKeys.put(key, id);
		Preconditions.checkState(prev == null, "Overwriting key with name %s", id);
		return new DataStoreBuilder<K, V>(this, key, keyClass, valueClass);
	}

	<K, V> void register(DataStoreKey<K, V> key, DataStoreWrapper<K, V> meta) {
		checkKeyExists(key);
		DataStoreWrapper<?, ?> prev = dataStoreMeta.put(key, meta);
		Preconditions.checkState(prev == null, "Overwriting wrapper for key %s", key);
	}

	public void validate() {
		Set<DataStoreKey<?, ?>> missing = Sets.difference(dataStoreKeys.keySet(), dataStoreMeta.keySet());
		Preconditions.checkState(missing.isEmpty(), "Keys [%s] were registered, but are not associated with any data", missing);
	}
}
