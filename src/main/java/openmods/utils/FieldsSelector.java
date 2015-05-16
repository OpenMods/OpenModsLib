package openmods.utils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

public abstract class FieldsSelector {

	public static class FieldEntry implements Comparable<FieldEntry> {
		public final Field field;
		public final int rank;

		public FieldEntry(Field field, int rank) {
			this.field = field;
			this.rank = rank;
		}

		@Override
		public int compareTo(FieldEntry o) {
			int result = Integer.compare(this.rank, o.rank);
			if (result != 0) return result;

			return this.field.getName().compareTo(o.field.getName());
		}
	}

	private final CachedFactory<Class<?>, Collection<Field>> cache = new CachedFactory<Class<?>, Collection<Field>>() {
		@Override
		protected Collection<Field> create(Class<?> key) {
			return scanForFields(key);
		}
	};

	protected abstract List<FieldEntry> listFields(Class<?> cls);

	private Collection<Field> scanForFields(Class<?> cls) {
		final List<FieldEntry> entries = listFields(cls);
		Collections.sort(entries);

		ImmutableList.Builder<Field> result = ImmutableList.builder();
		for (FieldEntry entry : entries) {
			final Field field = entry.field;
			result.add(field);
			field.setAccessible(true);
		}
		return result.build();
	}

	public Collection<Field> getFields(Class<?> cls) {
		synchronized (cache) {
			return cache.getOrCreate(cls);
		}
	}
}
