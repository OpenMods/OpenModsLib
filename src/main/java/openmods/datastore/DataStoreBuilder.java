package openmods.datastore;

import java.util.List;
import java.util.Map;

import openmods.utils.io.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DataStoreBuilder<K, V> {

	private final DataStoreManager owner;
	private final DataStoreKey<K, V> key;
	private final Class<? extends K> keyClass;
	private final Class<? extends V> valueClass;

	private IStreamWriteable<K> keyWriter;
	private IStreamWriteable<V> valueWriter;

	private IStreamReadable<K> keyReader;
	private IStreamReadable<V> valueReader;

	private List<IDataVisitor<K, V>> visitors = Lists.newArrayList();

	private final Map<K, V> values = Maps.newHashMap();

	DataStoreBuilder(DataStoreManager owner, DataStoreKey<K, V> key, Class<? extends K> keyClass, Class<? extends V> valueClass) {
		this.owner = owner;
		this.key = key;
		this.keyClass = keyClass;
		this.valueClass = valueClass;
	}

	public DataStoreKey<K, V> register() {
		Preconditions.checkNotNull(keyWriter, "Key writer not set");
		Preconditions.checkNotNull(valueWriter, "Value writer not set");
		Preconditions.checkNotNull(keyReader, "Key reader not set");
		Preconditions.checkNotNull(valueReader, "Value reader not set");

		final DataStoreWrapper<K, V> wrapper = new DataStoreWrapper<K, V>(values, keyWriter, valueWriter, keyReader, valueReader);

		for (IDataVisitor<K, V> visitor : visitors)
			wrapper.addVisitor(visitor);

		wrapper.activateLocalData();
		owner.register(key, wrapper);

		return key;
	}

	public void addEntry(K key, V value) {
		Preconditions.checkNotNull(key, "Null key not allowed");
		Preconditions.checkNotNull(value, "Null values not allowed");
		V prev = values.put(key, value);
		Preconditions.checkState(prev == null, "Replacing value for key %s: %s -> %s, id: %s", key, prev, value, this.key.id);
	}

	private <T> TypeRW<T> getDefaultReaderWriter(Class<? extends T> cls) {
		@SuppressWarnings("unchecked")
		TypeRW<T> rw = (TypeRW<T>)TypeRW.TYPES.get(cls);

		Preconditions.checkNotNull(rw, "Can't find default reader/writer for class %s, id: %s", cls, key.id);
		return rw;
	}

	public void setDefaultKeyWriter() {
		this.keyWriter = getDefaultReaderWriter(keyClass);
	}

	public void setDefaultValueWriter() {
		this.valueWriter = getDefaultReaderWriter(valueClass);
	}

	public void setDefaultKeyReader() {
		this.keyReader = getDefaultReaderWriter(keyClass);
	}

	public void setDefaultValueReader() {
		this.valueReader = getDefaultReaderWriter(valueClass);
	}

	public void setDefaultKeyReaderWriter() {
		setDefaultKeyWriter();
		setDefaultKeyReader();
	}

	public void setDefaultValueReaderWriter() {
		setDefaultValueWriter();
		setDefaultValueReader();
	}

	public void setDefaultReadersWriters() {
		setDefaultKeyReaderWriter();
		setDefaultValueReaderWriter();
	}

	public void setKeyWriter(IStreamWriteable<K> keyWriter) {
		this.keyWriter = keyWriter;
	}

	public void setValueWriter(IStreamWriteable<V> valueWriter) {
		this.valueWriter = valueWriter;
	}

	public void setKeyReaderWriter(IStreamSerializable<K> rw) {
		this.keyReader = rw;
		this.keyWriter = rw;
	}

	public void setKeyReader(IStreamReadable<K> keyReader) {
		this.keyReader = keyReader;
	}

	public void setValueReader(IStreamReadable<V> valueReader) {
		this.valueReader = valueReader;
	}

	public void setValueReaderWriter(IStreamSerializable<V> rw) {
		this.valueReader = rw;
		this.valueWriter = rw;
	}

	public void addVisitor(IDataVisitor<K, V> visitor) {
		visitors.add(visitor);
	}

}
