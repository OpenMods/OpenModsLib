package openmods.config.simple;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import openmods.Log;

public class ConfigProcessor {

	public interface UpdateListener {
		public void valueSet(String value);
	}

	private static class EntryMeta {
		public final int version;
		public final String value;
		public String[] comment;
		public final transient UpdateListener listener;

		public EntryMeta(String value, int version, UpdateListener listener) {
			this.version = version;
			this.value = value;
			this.listener = listener;
			this.comment = null;
		}

		public EntryMeta(String value, int version, UpdateListener listener, String... comment) {
			this.version = version;
			this.value = value;
			this.listener = listener;
			this.comment = comment;
		}

		@Override
		public String toString() {
			return "[value=" + value + ", version=" + version + ", comment=" + Arrays.toString(comment) + "]";
		}
	}

	private Map<String, EntryMeta> entries = Maps.newHashMap();

	public void addEntry(String name, int version, String defaultValue, UpdateListener listener, String... comment) {
		Preconditions.checkNotNull(listener);
		addEntry(name, new EntryMeta(defaultValue, version, listener, comment));
	}

	public void addEntry(String name, int version, String defaultValue, UpdateListener listener) {
		Preconditions.checkNotNull(listener);
		addEntry(name, new EntryMeta(defaultValue, version, listener));
	}

	private void addEntry(String name, EntryMeta meta) {
		EntryMeta prev = entries.put(name, meta);
		Preconditions.checkState(prev == null, "Duplicate property '%s': [%s, %s]", name, prev, meta);
	}

	private static class EntryCollection extends HashMap<String, EntryMeta> {
		private static final long serialVersionUID = -3851628207393131247L;
	}

	public void process(File config) {
		boolean modified = false;

		Log.debug("Parsing config file '%s'", config);
		Map<String, EntryMeta> values = readFile(config);
		if (values == null) values = Maps.newHashMap();

		for (String key : Sets.intersection(values.keySet(), entries.keySet())) {
			EntryMeta defaultEntry = entries.get(key);
			EntryMeta actualEntry = values.get(key);

			if (defaultEntry.version > actualEntry.version) {
				Log.warn("Config value '%s' replaced with newer version", key);
				values.put(key, defaultEntry);
				modified = true;
			} else {
				if (!Arrays.equals(defaultEntry.comment, actualEntry.comment)) {
					actualEntry.comment = defaultEntry.comment;
					modified = true;
				}

				defaultEntry.listener.valueSet(actualEntry.value);
			}
		}

		Set<String> removed = Sets.difference(values.keySet(), entries.keySet());

		if (!removed.isEmpty()) {
			Log.warn("Removing obsolete values: '%s'", removed);

			modified = true;
			for (String key : ImmutableSet.copyOf(removed))
				values.remove(key);
		}

		Set<String> added = Sets.difference(entries.keySet(), values.keySet());

		if (!added.isEmpty()) {
			Log.warn("Adding new values: '%s'", added);
			modified = true;

			for (String key : added) {
				final EntryMeta entry = entries.get(key);
				values.put(key, entry);
				entry.listener.valueSet(entry.value);
			}
		}

		if (modified) {
			writeFile(config, values);
		}
	}

	private static void writeFile(File output, Map<String, EntryMeta> values) {
		try {
			OutputStream stream = new FileOutputStream(output);

			try {
				Writer writer = new OutputStreamWriter(stream, Charsets.UTF_8);

				try {
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					gson.toJson(values, writer);
				} finally {
					writer.close();
				}

			} finally {
				stream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<String, EntryMeta> readFile(File input) {
		if (!input.exists()) return null;
		try {
			InputStream stream = new FileInputStream(input);

			try {
				Reader reader = new InputStreamReader(stream, Charsets.UTF_8);
				Gson gson = new Gson();
				return gson.fromJson(reader, EntryCollection.class);

			} finally {
				stream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
