package openmods.network.event;

import java.util.function.Supplier;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class NetworkEventManager {

	private static NetworkEventDispatcher DISPATCHER;

	public static NetworkEventDispatcher dispatcher() {
		return DISPATCHER;
	}

	private static class Callbacks extends CommonRegistryCallbacks<Class<? extends NetworkEvent>, NetworkEventEntry> {
		@Override
		protected Class<? extends NetworkEvent> getWrappedObject(NetworkEventEntry entry) {
			return entry.getPacketType();
		}
	}

	@SubscribeEvent
	public static void registerRegistry(RegistryEvent.NewRegistry e) {
		final IForgeRegistry<NetworkEventEntry> registry = new RegistryBuilder<NetworkEventEntry>()
				.setName(OpenMods.location("network_events"))
				.setType(NetworkEventEntry.class)
				.setIDRange(0, 0xFFFF) // something, something, 64k
				.addCallback(new Callbacks())
				.disableSaving()
				.create();

		DISPATCHER = new NetworkEventDispatcher(registry);
	}

	public static class RegistrationContext {
		private final IForgeRegistry<NetworkEventEntry> registry;

		public RegistrationContext(IForgeRegistry<NetworkEventEntry> registry) {
			this.registry = registry;
		}

		public <T extends NetworkEvent> RegistrationContext register(final ResourceLocation id, final Class<T> cls, final Supplier<T> ctor) {
			final NetworkEventMeta meta = cls.getAnnotation(NetworkEventMeta.class);
			final EventDirection direction = (meta != null)? meta.direction() : EventDirection.ANY;
			registry.register(new NetworkEventEntry() {
				@Override
				public EventDirection getDirection() {
					return direction;
				}

				@Override
				public NetworkEvent createInstance() {
					return ctor.get();
				}

				@Override
				public Class<? extends NetworkEvent> getPacketType() {
					return cls;
				}

				@Override
				public String toString() {
					return "Wrapper{" + cls + "}";
				}
			}.setRegistryName(id));

			return this;
		}

	}

	public static RegistrationContext startRegistration(IForgeRegistry<NetworkEventEntry> registry) {
		return new RegistrationContext(registry);
	}
}
