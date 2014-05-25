package openmods.network.event;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

public class EventPacketRegistry {

	private static final int TYPE_FIELD_FLAGS = Modifier.FINAL | Modifier.PUBLIC | Modifier.STATIC;

	private final TIntObjectHashMap<IEventPacketType> idToType = new TIntObjectHashMap<IEventPacketType>();

	private final Map<Class<? extends EventPacket>, IEventPacketType> typeToId = Maps.newIdentityHashMap();

	private final String modid;

	public EventPacketRegistry(String modid) {
		this.modid = modid;
	}

	public void registerType(Class<? extends EventPacket> packetType) {
		IEventPacketType type = getEventType(packetType);
		int typeId = type.getId();
		idToType.put(typeId, type);
		typeToId.put(packetType, type);
	}

	public IEventPacketType getTypeForClass(Class<? extends EventPacket> cls) {
		IEventPacketType result = typeToId.get(cls);
		Preconditions.checkNotNull(result, "Class %s is not registered for modid %s", cls, modid);
		return result;
	}

	public IEventPacketType getTypeForId(int id) {
		IEventPacketType result = idToType.get(id);
		Preconditions.checkNotNull(result, "Id %s is not registered for modid %s", id, modid);
		return result;
	}

	private static IEventPacketType getEventType(Class<? extends EventPacket> packetType) {
		try {
			IEventPacketType result;
			Field typeField = packetType.getDeclaredField("EVENT_TYPE");
			Preconditions.checkArgument(IEventPacketType.class.isAssignableFrom(typeField.getType()),
					"Field EVENT_TYPE in class %s has invalid type (must be %s)",
					packetType, IEventPacketType.class);

			int modifiers = typeField.getModifiers();
			Preconditions.checkArgument((modifiers & TYPE_FIELD_FLAGS) == TYPE_FIELD_FLAGS,
					"Field EVENT_TYPE in class %s must be public, final, static", packetType);
			result = (IEventPacketType)typeField.get(null);
			Preconditions.checkNotNull(result, "Field EVENT_TYPE in class %s has no value", packetType);
			return result;
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(String.format("Class %s has no EVENT_TYPE field", packetType));
		} catch (Throwable t) {
			throw Throwables.propagate(t);
		}
	}

}
