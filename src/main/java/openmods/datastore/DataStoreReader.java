package openmods.datastore;

import java.io.DataInput;
import java.io.IOException;
import java.util.Map;

import openmods.utils.ByteUtils;
import openmods.utils.io.IStreamReader;

import com.google.common.collect.Maps;

public class DataStoreReader<K, V> {
	private final IStreamReader<K> keyReader;
	private final IStreamReader<V> valueReader;
	private final DataStoreWrapper<K, V> wrapper;

	DataStoreReader(DataStoreWrapper<K, V> wrapper, IStreamReader<K> keyReader, IStreamReader<V> valueReader) {
		this.keyReader = keyReader;
		this.valueReader = valueReader;
		this.wrapper = wrapper;
	}

	public void read(DataInput input) {
		int size = ByteUtils.readVLI(input);
		Map<K, V> values = Maps.newHashMap();

		try {
			for (int i = 0; i < size; i++) {
				K key = keyReader.readFromStream(input);
				V value = valueReader.readFromStream(input);
				values.put(key, value);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		DataStore<K, V> result = new DataStore<K, V>(values);
		wrapper.activateData(result);
	}
}
