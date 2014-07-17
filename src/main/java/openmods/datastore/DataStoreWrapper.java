package openmods.datastore;

import java.util.Map;
import java.util.Set;

import openmods.utils.io.IStreamReadable;
import openmods.utils.io.IStreamWriteable;

import com.google.common.collect.Sets;

public class DataStoreWrapper<K, V> {

	private final DataStore<K, V> localData;

	private DataStore<K, V> activeData;

	private final Set<IDataVisitor<K, V>> visitors = Sets.newIdentityHashSet();

	private final IStreamWriteable<K> keyWriter;
	private final IStreamWriteable<V> valueWriter;

	private final IStreamReadable<K> keyReader;
	private final IStreamReadable<V> valueReader;

	DataStoreWrapper(Map<K, V> localData, IStreamWriteable<K> keyWriter, IStreamWriteable<V> valueWriter, IStreamReadable<K> keyReader, IStreamReadable<V> valueReader) {
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
