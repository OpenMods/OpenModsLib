package openmods.injector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import openmods.Log;

public class InjectedClassesManager {

	public static final String GENERATED_CLS_PREFIX = "$\u2697$";

	public static final String GENERATED_CLS_SEPARATOR = "\u261E";

	public static final InjectedClassesManager instance = new InjectedClassesManager();

	private final Map<String, IClassBytesProvider> providers = Maps.newHashMap();

	public void registerProvider(String providerId, IClassBytesProvider provider) {
		IClassBytesProvider prev = providers.put(providerId, provider);
		Preconditions.checkState(prev == null, "Duplicate provider registered, %s: %s -> %s", providerId, prev, providerId);
	}

	public String createClassName(String providerId, String arg) {
		Preconditions.checkState(providers.containsKey(providerId), "Unknown provider: %s", providerId);
		return GENERATED_CLS_PREFIX + providerId + GENERATED_CLS_SEPARATOR + arg;
	}

	public byte[] tryGetBytecode(String clsName) {
		if (!clsName.startsWith(GENERATED_CLS_PREFIX)) return null;
		String[] parts = clsName.substring(GENERATED_CLS_PREFIX.length()).split(GENERATED_CLS_SEPARATOR);
		if (parts.length != 2) {
			Log.warn("Malformed generated class: %s", clsName);
			return null;
		}

		String providerId = parts[0];
		IClassBytesProvider provider = providers.get(providerId);
		if (provider == null) {
			Log.warn("Unknown provider: %s", providerId);
			return null;
		}

		String arg = parts[1];

		try {
			return provider.getClassBytes(clsName, arg);
		} catch (Throwable t) {
			Log.severe(t, "Failed to run provider %s, clsName: %s", provider, clsName);
			return null;
		}
	}
}
