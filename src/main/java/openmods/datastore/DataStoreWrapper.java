package openmods.datastore;

import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import openmods.utils.io.IStreamReader;
import openmods.utils.io.IStreamWriter;

public class DataStoreWrapper<K, V> {

	private final DataStore<K, V> localData;

	private DataStore<K, V> activeData;

	private final Set<IDataVisitor<K, V>> visitors = Sets.newIdentityHashSet();

	private final IStreamWriter<K> keyWriter;
	private final IStreamWriter<V> valueWriter;

	private final IStreamReader<K> keyReader;
	private final IStreamReader<V> valueReader;

	DataStoreWrapper(Map<K, V> localData, IStreamWriter<K> keyWriter, IStreamWriter<V> valueWriter, IStreamReader<K> keyReader, IStreamReader<V> valueReader) {
		this.localData = new DataStore<K, V>(localData);
		this.keyWriter = keyWriter;
		this.valueWriter = valueWriter;
		this.keyReader = keyReader;
		this.valueReader = valueReader;
	}

	private void notifyVisitors() {
		for (IDataVisitor<K, V> visitor : visitors)
			activeData.visit(visitor);
	}

	public void activateData(DataStore<K, V> data) {
		this.activeData = data;
		notifyVisitors();
	}

	public void activateLocalData() {
		this.activeData = localData;
		notifyVisitors();
	}

	public DataStoreReader<K, V> createReader() {
		return new DataStoreReader<K, V>(this, keyReader, valueReader);
	}

	public DataStoreWriter<K, V> createWriter() {
		return new DataStoreWriter<K, V>(localData, keyWriter, valueWriter);
	}

	public DataStore<K, V> localData() {
		return localData;
	}

	public DataStore<K, V> activeData() {
		return activeData;
	}

	public void addVisitor(IDataVisitor<K, V> visitor) {
		visitors.add(visitor);
	}
}
