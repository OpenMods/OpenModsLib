package openmods.network.event;

import openmods.datastore.DataStoreBuilder;
import openmods.datastore.IDataVisitor;
import openmods.network.IdSyncManager;
import openmods.utils.io.TypeRW;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;

public class NetworkEventManager {

	public static class RegistrationContext {
		private int currentId = 0;

		private DataStoreBuilder<String, Integer> builder;

		private RegistrationContext() {
			this.builder = IdSyncManager.INSTANCE.createDataStore("events", String.class, Integer.class);
		}

		public RegistrationContext register(Class<? extends NetworkEvent> cls) {
			Preconditions.checkState(builder != null, "This object no longer can register new events");
			builder.addEntry(cls.getName(), currentId++);
			return this;
		}

		void register(IDataVisitor<String, Integer> eventIdVisitor) {
			builder.setDefaultKeyReaderWriter();
			builder.setValueReaderWriter(TypeRW.VLI_SERIALIZABLE);
			builder.addVisitor(eventIdVisitor);
			builder.register();
			builder = null;
		}
	}

	private NetworkEventManager() {}

	public static final NetworkEventManager INSTANCE = new NetworkEventManager();

	private final NetworkEventRegistry registry = new NetworkEventRegistry();

	private final NetworkEventDispatcher dispatcher = new NetworkEventDispatcher(registry);

	private RegistrationContext registrationContext;

	public RegistrationContext startRegistration() {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.PREINITIALIZATION), "This method can only be called in pre-initialization state");

		if (registrationContext == null) registrationContext = new RegistrationContext();
		return registrationContext;
	}

	public void finalizeRegistration() {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.POSTINITIALIZATION), "This method can only be called in post-initialization state");

		if (registrationContext != null) {
			registrationContext.register(registry);
			registrationContext = null;
		}
	}

	public NetworkEventDispatcher dispatcher() {
		return dispatcher;
	}
}
