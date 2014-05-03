package openmods.utils;

import openmods.Log;

public class SafeClassLoad {
	public final String clsName;
	private Class<?> loaded;

	public SafeClassLoad(String clsName) {
		this.clsName = clsName;
	}

	public void load() {
		if (loaded == null) loaded = ReflectionHelper.getClass(clsName);
	}

	public Class<?> get() {
		load();
		return loaded;
	}

	public boolean tryLoad() {
		return tryLoad(true);
	}

	public boolean tryLoad(boolean silent) {
		try {
			load();
			return true;
		} catch (Throwable t) {
			if (!silent) Log.warn(t, "Loading class %s failed", clsName);
			return false;
		}
	}

	@Override
	public String toString() {
		return "delayed " + clsName;
	}
}