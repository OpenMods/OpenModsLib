package openmods.network.event;

import com.google.common.base.Throwables;
import java.lang.reflect.Constructor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.RegistryBuilder;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;
import openmods.utils.RegistrationContextBase;

@EventBusSubscriber
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
				.create();

		DISPATCHER = new NetworkEventDispatcher(registry);
	}

	public static class RegistrationContext extends RegistrationContextBase<NetworkEventEntry> {

		public RegistrationContext(IForgeRegistry<NetworkEventEntry> registry, String domain) {
			super(registry, domain);
		}

		public RegistrationContext(IForgeRegistry<NetworkEventEntry> registry) {
			super(registry);
		}

		public RegistrationContext register(final Class<? extends NetworkEvent> cls) {
			final NetworkEventMeta meta = cls.getAnnotation(NetworkEventMeta.class);

			final EventDirection direction = (meta != null)? meta.direction() : EventDirection.ANY;

			final Constructor<? extends NetworkEvent> ctor;
			try {
				ctor = cls.getConstructor();
			} catch (Exception e) {
				throw new IllegalArgumentException("Class " + cls + " has no parameterless constructor");
			}

			final ResourceLocation eventId = new ResourceLocation(domain, cls.getName());

			registry.register(new NetworkEventEntry() {
				@Override
				public EventDirection getDirection() {
					return direction;
				}

				@Override
				public NetworkEvent createPacket() {
					try {
						return ctor.newInstance();
					} catch (Exception e) {
						throw Throwables.propagate(e);
					}
				}

				@Override
				public Class<? extends NetworkEvent> getPacketType() {
					return cls;
				}

				@Override
				public String toString() {
					return "Wrapper{" + cls + "}";
				}
			}.setRegistryName(eventId));

			return this;
		}

	}

	public static RegistrationContext startRegistration(IForgeRegistry<NetworkEventEntry> registry) {
		return new RegistrationContext(registry);
	}

	public static RegistrationContext startRegistration(IForgeRegistry<NetworkEventEntry> registry, String domain) {
		return new RegistrationContext(registry, domain);
	}
}
