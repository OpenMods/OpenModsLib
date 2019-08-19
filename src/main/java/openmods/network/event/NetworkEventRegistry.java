package openmods.network.event;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Map;
import openmods.datastore.IDataVisitor;

public class NetworkEventRegistry implements IDataVisitor<String, Integer> {

	private Map<String, Class<? extends NetworkEvent>> knownClasses = ImmutableMap.of();

	private final TIntObjectHashMap<INetworkEventType> idToType = new TIntObjectHashMap<INetworkEventType>();

	private final Map<Class<? extends NetworkEvent>, Integer> clsToId = Maps.newIdentityHashMap();

	public NetworkEventRegistry() {}

	int getIdForClass(Class<? extends NetworkEvent> cls) {
		Integer result = clsToId.get(cls);
		Preconditions.checkNotNull(result, "Class %s is not registered", cls);
		return result;
	}

	INetworkEventType getTypeForId(int id) {
		INetworkEventType result = idToType.get(id);
		Preconditions.checkNotNull(result, "Id %s is not registered", id);
		return result;
	}

	@Override
	public void begin(int size) {
		idToType.clear();
		clsToId.clear();
	}

	public void registerClasses(final Map<String, Class<? extends NetworkEvent>> events) {
		this.knownClasses = ImmutableMap.copyOf(events);
	}

	public static INetworkEventType createPacketType(final Class<? extends NetworkEvent> cls) {
		final NetworkEventMeta meta = cls.getAnnotation(NetworkEventMeta.class);
		final NetworkEventCustomType customType = cls.getAnnotation(NetworkEventCustomType.class);

		if (customType != null) {
			Preconditions.checkState(meta == null, "NetworkEventMeta and NetworkEventCustomType are mutually exclusive");
			try {
				return customType.value().newInstance();
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		}

		final boolean isCompressed;
		final boolean isChunked;
		final EventDirection direction;

		if (meta != null) {
			isChunked = meta.chunked();
			isCompressed = meta.compressed();
			direction = meta.direction();
		} else {
			isChunked = false;
			isCompressed = false;
			direction = EventDirection.ANY;
		}

		return new INetworkEventType() {
			@Override
			public boolean isCompressed() {
				return isCompressed;
			}

			@Override
			public boolean isChunked() {
				return isChunked;
			}

			@Override
			public EventDirection getDirection() {
				return direction;
			}

			@Override
			public NetworkEvent createPacket() {
				try {
					return cls.newInstance();
				} catch (Exception e) {
					throw Throwables.propagate(e);
				}
			}
		};
	}

	@Override
	public void entry(String clsKey, Integer eventId) {
		Class<? extends NetworkEvent> cls = knownClasses.get(clsKey);
		if (cls == null) {
			throw new IllegalArgumentException("Unknown event class " + clsKey);
		}
		INetworkEventType type = createPacketType(cls);
		idToType.put(eventId, type);
		clsToId.put(cls, eventId);
	}

	@Override
	public void end() {}
}
