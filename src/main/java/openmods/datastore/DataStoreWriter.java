package openmods.datastore;

import java.io.IOException;
import net.minecraft.network.PacketBuffer;
import openmods.utils.io.IStreamWriter;

public class DataStoreWriter<K, V> {
	private final DataStore<K, V> data;
	private final IStreamWriter<K> keyWriter;
	private final IStreamWriter<V> valueWriter;

	DataStoreWriter(DataStore<K, V> data, IStreamWriter<K> keyWriter, IStreamWriter<V> valueWriter) {
		this.data = data;
		this.keyWriter = keyWriter;
		this.valueWriter = valueWriter;
	}

	public void write(final PacketBuffer output) {
		data.visit(new IDataVisitor<K, V>() {
			@Override
			public void begin(int size) {
				output.writeVarIntToBuffer(size);
			}

			@Override
			public void entry(K key, V value) {
				try {
					keyWriter.writeToStream(key, output);
					valueWriter.writeToStream(value, output);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public void end() {}
		});
	}
}
