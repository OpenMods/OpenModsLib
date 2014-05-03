package openmods.integration;

import java.util.List;

import openmods.Log;

import com.google.common.collect.Lists;

public class Integration {

	private static final List<IIntegrationModule> modules = Lists.newArrayList();

	private static boolean alreadyLoaded;

	public static void addModule(IIntegrationModule module) {
		if (alreadyLoaded) Log.warn("Trying to add integration module %s after loading. This will not work");
		modules.add(module);
	}

	public static void loadModules() {
		if (alreadyLoaded) {
			Log.warn("Trying to load integration modules twice, ignoring");
			return;
		}

		for (IIntegrationModule module : modules) {
			try {
				if (module.canLoad()) {
					module.load();
					Log.info("Loaded integration module '%s'", module.name());
				} else {
					Log.info("Condition no met for integration module '%s', not loading", module.name());
				}
			} catch (Throwable t) {
				Log.warn(t, "Can't load integration module '%s'", module.name());
			}
		}
	}
}
