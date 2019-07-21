package openmods.utils.bitmap;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.Direction;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;

public class BitMapUtils {

	public static <T> Iterator<Map.Entry<T, Boolean>> createFlagIterator(final IReadableBitMap<T> map, final Iterable<T> values) {
		return createFlagIterator(map, values.iterator());
	}

	public static <T> Iterator<Map.Entry<T, Boolean>> createFlagIterator(final IReadableBitMap<T> map, final Iterator<T> values) {
		return new UnmodifiableIterator<Map.Entry<T, Boolean>>() {
			@Override
			public boolean hasNext() {
				return values.hasNext();
			}

			@Override
			public Entry<T, Boolean> next() {
				final T key = values.next();
				final boolean value = map.get(key);
				return Maps.immutableEntry(key, value);
			}
		};
	}

	public static <T> Iterator<T> createTrueValuesIterator(final IReadableBitMap<T> map, final Iterable<T> values) {
		return createTrueValuesIterator(map, values.iterator());
	}

	public static <T> Iterator<T> createTrueValuesIterator(final IReadableBitMap<T> map, final Iterator<T> values) {
		return new AbstractIterator<T>() {
			@Override
			protected T computeNext() {
				while (values.hasNext()) {
					final T key = values.next();
					if (map.get(key)) return key;
				}

				return endOfData();
			}
		};
	}

	public static <T> IValueProvider<Boolean> singleBitProvider(final IReadableBitMap<T> map, final T key) {
		return () -> map.get(key);
	}

	public static <T> IValueReceiver<Boolean> singleBitReceiver(final IWriteableBitMap<T> map, final T key) {
		return value -> map.set(key, value);
	}

	public static IValueReceiver<Boolean> singleBitReceiver(final IRpcIntBitMap map, final int key) {
		return value -> map.set(key, value);
	}

	public static IWriteableBitMap<Direction> createRpcAdapter(final IRpcDirectionBitMap map) {
		return new IWriteableBitMap<Direction>() {
			@Override
			public void toggle(Direction value) {
				map.toggle(value);
			}

			@Override
			public void set(Direction key, boolean value) {
				map.set(key, value);
			}

			@Override
			public void mark(Direction value) {
				map.mark(value);
			}

			@Override
			public void clearAll() {
				map.clearAll();
			}

			@Override
			public void clear(Direction value) {
				map.clear(value);
			}
		};
	}

	public static IWriteableBitMap<Integer> createRpcAdapter(final IRpcIntBitMap map) {
		return new IWriteableBitMap<Integer>() {
			@Override
			public void toggle(Integer value) {
				map.toggle(value);
			}

			@Override
			public void set(Integer key, boolean value) {
				map.set(key, value);
			}

			@Override
			public void mark(Integer value) {
				map.mark(value);
			}

			@Override
			public void clearAll() {
				map.clearAll();
			}

			@Override
			public void clear(Integer value) {
				map.clear(value);
			}
		};
	}
}
