package openmods.datastore;

public final class DataStoreKey<K, V> {

	public final String id;

	DataStoreKey(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return String.format("<key id = %s, hash = 0x%X>", id, System.identityHashCode(this));
	}

}
