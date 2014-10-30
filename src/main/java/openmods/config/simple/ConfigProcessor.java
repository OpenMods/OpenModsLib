package openmods.config.simple;

import java.io.*;
import java.util.*;

import openmods.Log;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigProcessor {

	public interface ValueSink {
		public void valueParsed(String name, String value);
	}

	private static class EntryMeta {
		public int version;
		public String value;

		public EntryMeta(String value, int version) {
			this.version = version;
			this.value = value;
		}

		@Override
		public String toString() {
			return "[value=" + value + "]";
		}
	}

	private static class EntryMetaWithComment extends EntryMeta {
		public String[] comment;

		public EntryMetaWithComment(String value, int version, String... comment) {
			super(value, version);
			this.comment = comment;
		}

		@Override
		public String toString() {
			return "[value=" + value + ", comment=" + Arrays.toString(comment) + "]";
		}
	}

	private Map<String, EntryMeta> entries = Maps.newHashMap();

	public void addEntry(String name, String defaultValue, int version, String... comment) {
		addEntry(name, new EntryMetaWithComment(defaultValue, version, comment));
	}

	public void addEntry(String name, int version, String defaultValue) {
		addEntry(name, new EntryMeta(defaultValue, version));
	}

	private void addEntry(String name, final EntryMeta meta) {
		EntryMeta prev = entries.put(name, meta);
		Preconditions.checkState(prev == null, "Duplicate property %s: [%s, %s]", name, prev, meta);
	}

	private static class EntryCollection extends HashMap<String, EntryMeta> {
		private static final long serialVersionUID = -3851628207393131247L;
	}

	public void process(File config, ValueSink sink) {
		boolean modified = false;

		Log.info("Parsing config file %s", config);
		Map<String, EntryMeta> values = readFile(config);

		for (String key : Sets.intersection(values.keySet(), entries.keySet())) {
			EntryMeta defaultEntry = entries.get(key);
			EntryMeta actualEntry = values.get(key);

			if (defaultEntry.version > actualEntry.version) {
				Log.warn("Config value %s replaced with newer version", key);
				values.put(key, defaultEntry);
				modified = true;
			} else {
				sink.valueParsed(key, actualEntry.value);
			}
		}

		Set<String> removed = Sets.difference(values.keySet(), entries.keySet());

		if (!removed.isEmpty()) {
			Log.warn("Removing obsolete values: %s", removed);

			modified = true;
			for (String key : ImmutableSet.copyOf(removed))
				values.remove(key);
		}

		Set<String> added = Sets.difference(entries.keySet(), values.keySet());

		if (!added.isEmpty()) {
			Log.warn("Adding new values: %s", added);
			modified = true;

			for (String key : added)
				values.put(key, entries.get(key));
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
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				gson.toJson(values, writer);

			} finally {
				stream.close();
			}
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	private static Map<String, EntryMeta> readFile(File input) {
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
			throw Throwables.propagate(e);
		}
	}
}
