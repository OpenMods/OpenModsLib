package openmods.network.event;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.lang.reflect.Constructor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.RegistryBuilder;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;

public class NetworkEventManager {

	private NetworkEventManager() {}

	public static final NetworkEventManager INSTANCE = new NetworkEventManager();

	private static class Callbacks extends CommonRegistryCallbacks<Class<? extends NetworkEvent>, NetworkEventEntry> {
		@Override
		protected Class<? extends NetworkEvent> getWrappedObject(NetworkEventEntry entry) {
			return entry.getPacketType();
		}
	}

	final IForgeRegistry<NetworkEventEntry> registry = new RegistryBuilder<NetworkEventEntry>()
			.setName(OpenMods.location("network_events"))
			.setType(NetworkEventEntry.class)
			.setIDRange(0, 0xFFFF) // something, something, 64k
			.addCallback(new Callbacks())
			.create();

	private final NetworkEventDispatcher dispatcher = new NetworkEventDispatcher(registry);

	public NetworkEventDispatcher dispatcher() {
		return dispatcher;
	}

	public NetworkEventManager register(Class<? extends NetworkEvent> cls) {
		ModContainer mc = Loader.instance().activeModContainer();
		Preconditions.checkState(mc != null, "This method can be only used during mod initialization");
		String prefix = mc.getModId().toLowerCase();
		return register(prefix, cls);
	}

	public NetworkEventManager register(String domain, final Class<? extends NetworkEvent> cls) {
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
