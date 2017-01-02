package openmods.datastore;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import net.minecraft.network.PacketBuffer;
import openmods.utils.io.IStreamReader;

public class DataStoreReader<K, V> {
	private final IStreamReader<K> keyReader;
	private final IStreamReader<V> valueReader;
	private final DataStoreWrapper<K, V> wrapper;

	DataStoreReader(DataStoreWrapper<K, V> wrapper, IStreamReader<K> keyReader, IStreamReader<V> valueReader) {
		this.keyReader = keyReader;
		this.valueReader = valueReader;
		this.wrapper = wrapper;
	}

	public void read(PacketBuffer input) {
		final int size = input.readVarIntFromBuffer();
		final Map<K, V> values = Maps.newHashMap();

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
