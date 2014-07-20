package openmods.utils.bitmap;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;

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

}
