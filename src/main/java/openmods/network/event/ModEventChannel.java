package openmods.network.event;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Map;

import openmods.datastore.IDataVisitor;
import openmods.network.ExtendedOutboundHandler;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;

public class ModEventChannel implements IDataVisitor<String, Integer> {

	private final TIntObjectHashMap<INetworkEventType> idToType = new TIntObjectHashMap<INetworkEventType>();

	private final Map<Class<? extends NetworkEvent>, Integer> clsToId = Maps.newIdentityHashMap();

	private final Map<Side, FMLEmbeddedChannel> channels;

	private final NetworkEventDispatcher dispatcher;

	public final String modId;

	public ModEventChannel(String modId, NetworkEventDispatcher dispatcher) {
		this.modId = modId;
		this.dispatcher = dispatcher;

		NetworkEventCodec codec = new NetworkEventCodec(this);

		String channelId = modId + "|E";
		this.channels = NetworkRegistry.INSTANCE.newChannel(channelId, codec, new NetworkEventInboundHandler());

		ExtendedOutboundHandler.install(this.channels);
	}

	int getIdForClass(Class<? extends NetworkEvent> cls) {
		Integer result = clsToId.get(cls);
		Preconditions.checkNotNull(result, "Class %s is not registered for modid %s", cls, modId);
		return result;
	}

	INetworkEventType getTypeForId(int id) {
		INetworkEventType result = idToType.get(id);
		Preconditions.checkNotNull(result, "Id %s is not registered for modid %s", id, modId);
		return result;
	}

	public FMLEmbeddedChannel channel(Side side) {
		return channels.get(side);
	}

	@Override
	public void begin(int size) {
		for (Class<? extends NetworkEvent> cls : clsToId.keySet())
			dispatcher.unregister(cls);

		idToType.clear();
		clsToId.clear();
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
		Class<?> candidateCls;
		try {
			candidateCls = Class.forName(clsKey);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(String.format("Can't find class %s", clsKey), e);
		}

		Preconditions.checkArgument(NetworkEvent.class.isAssignableFrom(candidateCls));

		@SuppressWarnings("unchecked")
		Class<? extends NetworkEvent> cls = (Class<? extends NetworkEvent>)candidateCls;

		INetworkEventType type = createPacketType(cls);
		idToType.put(eventId, type);
		clsToId.put(cls, eventId);
		dispatcher.register(cls, this);
	}

	@Override
	public void end() {}
}
