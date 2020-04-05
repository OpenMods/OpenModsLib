package openmods.source;

import java.net.URL;
import openmods.Log;

public class ClassSourceCollector {

	public static class ClassMeta {
		public final Class<?> cls;

		public final URL loadedSource;

		// TODO 1.14 Consider restoring
		// public final Map<File, Set<String>> providerMods;

		public ClassMeta(Class<?> cls, URL loadedSource) {
			this.cls = cls;
			this.loadedSource = loadedSource;
		}

		public String source() {
			return loadedSource != null? loadedSource.toString() : "?";
		}
	}

	public ClassMeta getClassInfo(String clsName) throws ClassNotFoundException {
		Class<?> cls = Class.forName(clsName);
		return getClassInfo(cls);
	}

	public ClassMeta getClassInfo(Class<?> cls) {
		final Package pkg = cls.getPackage();

		URL loadedFrom = null;

		try {
			loadedFrom = cls.getProtectionDomain().getCodeSource().getLocation();
		} catch (Throwable t) {
			Log.warn(t, "Failed to get source for %s", cls);
		}

		return new ClassMeta(cls, loadedFrom);
	}
}
