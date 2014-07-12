package openmods.network.event;

import java.util.Map;

import openmods.datastore.DataStoreBuilder;
import openmods.datastore.DataStoreKey;
import openmods.datastore.IDataVisitor;
import openmods.network.IdSyncManager;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.ModContainer;

public class NetworkEventManager {

	public static final String CHANNEL_NAME = "OpenMods|I";

	public static class RegistrationContext {
		private int currentId = (int)(System.nanoTime() % 100);

		private DataStoreBuilder<String, Integer> builder;

		private RegistrationContext(DataStoreBuilder<String, Integer> builder) {
			this.builder = builder;
		}

		public RegistrationContext register(Class<? extends NetworkEvent> cls) {
			Preconditions.checkState(builder != null, "This object no longer can register new events");
			builder.addEntry(cls.getName(), currentId++);
			return this;
		}

		DataStoreKey<String, Integer> register(IDataVisitor<String, Integer> eventIdVisitor) {
			builder.setDefaultReadersWriters();
			builder.addVisitor(eventIdVisitor);
			DataStoreKey<String, Integer> result = builder.register();
			builder = null;
			return result;
		}
	}

	private NetworkEventManager() {}

	public static final NetworkEventManager INSTANCE = new NetworkEventManager();

	private final NetworkEventDispatcher dispatcher = new NetworkEventDispatcher();

	private final Map<String, RegistrationContext> registries = Maps.newHashMap();

	public RegistrationContext startRegistration() {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.PREINITIALIZATION), "This method can only be called in pre-initialization state");
		ModContainer container = Loader.instance().activeModContainer();
		Preconditions.checkNotNull(container, "This method can only be called in during mod initialization");

		String modId = container.getModId();
		RegistrationContext registry = registries.get(modId);
		if (registry == null) {
			final DataStoreBuilder<String, Integer> builder = IdSyncManager.INSTANCE.createDataStore("events", modId, String.class, Integer.class);
			registry = new RegistrationContext(builder);
			registries.put(modId, registry);
		}

		return registry;
	}

	public void finalizeRegistration() {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.POSTINITIALIZATION), "This method can only be called in post-initialization state");
		for (Map.Entry<String, RegistrationContext> e : registries.entrySet()) {
			final ModEventChannel channel = new ModEventChannel(e.getKey(), dispatcher);
			e.getValue().register(channel);
		}

		registries.clear();
	}

	public NetworkEventDispatcher dispatcher() {
		return dispatcher;
	}
}
