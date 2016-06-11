package openmods.integration;

import com.google.common.collect.Lists;
import java.util.List;
import openmods.Log;

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
					Log.debug("Loaded integration module '%s'", module.name());
				} else {
					Log.debug("Condition no met for integration module '%s', not loading", module.name());
				}
			} catch (Throwable t) {
				Log.warn(t, "Can't load integration module '%s'", module.name());
			}
		}
	}
}
