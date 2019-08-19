package openmods.network.event;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import java.util.Map;
import openmods.datastore.DataStoreBuilder;
import openmods.network.IdSyncManager;
import openmods.utils.io.TypeRW;

public class NetworkEventManager {

	public static class RegistrationContext {
		private int currentId = 0;

		private final DataStoreBuilder<String, Integer> builder;

		private final Map<String, Class<? extends NetworkEvent>> events = Maps.newHashMap();

		private RegistrationContext() {
			this.builder = IdSyncManager.INSTANCE.createDataStore("events", String.class, Integer.class);

			this.builder.setDefaultKeyReaderWriter();
			this.builder.setValueReaderWriter(TypeRW.VLI_SERIALIZABLE);
		}

		public RegistrationContext register(Class<? extends NetworkEvent> cls) {
			Preconditions.checkState(Loader.instance().isInState(LoaderState.PREINITIALIZATION), "This method can only be called in pre-initialization state");

			final String id = cls.getName();
			builder.addEntry(id, currentId++);
			events.put(id, cls);
			return this;
		}

		void register(NetworkEventRegistry eventIdVisitor) {
			eventIdVisitor.registerClasses(events);
			builder.addVisitor(eventIdVisitor);
			builder.register();
		}
	}

	private NetworkEventManager() {}

	public static final NetworkEventManager INSTANCE = new NetworkEventManager();

	private final NetworkEventRegistry registry = new NetworkEventRegistry();

	private final NetworkEventDispatcher dispatcher = new NetworkEventDispatcher(registry);

	private RegistrationContext registrationContext = new RegistrationContext();

	public RegistrationContext startRegistration() {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.PREINITIALIZATION), "This method can only be called in pre-initialization state");
		return registrationContext;
	}

	public void finalizeRegistration() {
		registrationContext.register(registry);
		registrationContext = null;
	}

	public NetworkEventDispatcher dispatcher() {
		return dispatcher;
	}
}
