package openmods.network.event;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import java.lang.reflect.Constructor;
import java.util.Map;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import net.minecraftforge.fml.common.registry.RegistryBuilder;
import openmods.OpenMods;

public class NetworkEventManager {

	private NetworkEventManager() {}

	private static final ResourceLocation CLASS_TO_ENTRY = OpenMods.location("class_to_entry");

	private static final ResourceLocation ENTRY_TO_ID = OpenMods.location("entry_to_id");

	public static final NetworkEventManager INSTANCE = new NetworkEventManager();

	private static class Callbacks implements IForgeRegistry.AddCallback<NetworkEventEntry>, IForgeRegistry.ClearCallback<NetworkEventEntry>, IForgeRegistry.CreateCallback<NetworkEventEntry> {

		@Override
		@SuppressWarnings("unchecked")
		public void onCreate(Map<ResourceLocation, ?> slaveset, BiMap<ResourceLocation, ? extends IForgeRegistry<?>> registries) {
			final Map<Class<? extends NetworkEvent>, NetworkEventEntry> classToEntryMap = Maps.newHashMap();
			((Map<ResourceLocation, Object>)slaveset).put(CLASS_TO_ENTRY, classToEntryMap);

			final BiMap<NetworkEventEntry, Integer> entryToId = HashBiMap.create();
			((Map<ResourceLocation, Object>)slaveset).put(ENTRY_TO_ID, entryToId);
		}

		@Override
		@SuppressWarnings("unchecked")
		public void onClear(IForgeRegistry<NetworkEventEntry> is, Map<ResourceLocation, ?> slaveset) {
			((Map<Class<? extends NetworkEvent>, NetworkEventEntry>)slaveset.get(CLASS_TO_ENTRY)).clear();
			((BiMap<NetworkEventEntry, Integer>)slaveset.get(ENTRY_TO_ID)).clear();
		}

		@Override
		@SuppressWarnings("unchecked")
		public void onAdd(NetworkEventEntry obj, int id, Map<ResourceLocation, ?> slaveset) {
			((Map<Class<? extends NetworkEvent>, NetworkEventEntry>)slaveset.get(CLASS_TO_ENTRY)).put(obj.getPacketType(), obj);
			((BiMap<NetworkEventEntry, Integer>)slaveset.get(ENTRY_TO_ID)).put(obj, id);
		}

	}

	@SuppressWarnings("unchecked")
	Map<Class<? extends NetworkEvent>, NetworkEventEntry> getClassToEntryMap() {
		return registry.getSlaveMap(CLASS_TO_ENTRY, Map.class);
	}

	@SuppressWarnings("unchecked")
	BiMap<NetworkEventEntry, Integer> getEventIdMap() {
		return registry.getSlaveMap(ENTRY_TO_ID, BiMap.class);
	}

	final IForgeRegistry<NetworkEventEntry> registry = new RegistryBuilder<NetworkEventEntry>()
			.setName(OpenMods.location("network_events"))
			.setType(NetworkEventEntry.class)
			.setIDRange(0, 0xFFFF) // something, something, 64k
			.addCallback(new Callbacks())
			.create();

	private final NetworkEventDispatcher dispatcher = new NetworkEventDispatcher(this);

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
