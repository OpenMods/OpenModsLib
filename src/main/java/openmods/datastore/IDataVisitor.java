package openmods.datastore;

public interface IDataVisitor<K, V> {
	public void begin(int size);

	public void entry(K key, V value);

	public void end();
}
