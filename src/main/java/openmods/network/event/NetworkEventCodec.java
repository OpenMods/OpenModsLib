package openmods.network.event;

import com.google.common.base.Preconditions;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import java.io.IOException;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.registries.IForgeRegistry;
import openmods.Log;
import openmods.utils.CommonRegistryCallbacks;

@Sharable
public class NetworkEventCodec {
	private final IForgeRegistry<NetworkEventEntry> registry;

	public NetworkEventCodec(IForgeRegistry<NetworkEventEntry> registry) {
		this.registry = registry;
	}

	PacketBuffer encode(NetworkEvent msg, LogicalSide side) throws IOException {
		final NetworkEventEntry entry = CommonRegistryCallbacks.getObjectToEntryMap(registry).get(msg.getClass());
		Preconditions.checkState(entry != null, "Can't find registration for class %s", msg.getClass());
		final int id = CommonRegistryCallbacks.getEntryIdMap(registry).get(entry);

		final EventDirection validator = entry.getDirection();
		Preconditions.checkState(validator != null && validator.validateSend(side),
				"Invalid direction: sending packet %s on side %s", msg.getClass(), side);

		final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
		buf.writeVarInt(id);
		msg.writeToStream(buf);
		return buf;
	}

	NetworkEvent decode(PacketBuffer payload, net.minecraftforge.fml.network.NetworkEvent.Context context) throws IOException {
		final int typeId = payload.readVarInt();
		final NetworkEventEntry type = CommonRegistryCallbacks.getEntryIdMap(registry).inverse().get(typeId);

		final EventDirection validator = type.getDirection();
		final LogicalSide side = context.getDirection().getReceptionSide();
		Preconditions.checkState(validator != null && validator.validateReceive(side),
				"Invalid direction: receiving packet %s on side %s", registry.getKey(type), side);

		final NetworkEvent event = type.createInstance();
		event.readFromStream(payload);

		event.context = context;
		event.sender = context.getSender();
		final int bufferJunkSize = payload.readableBytes();
		if (bufferJunkSize > 0) Log.warn("%s junk bytes left in buffer, event %s", bufferJunkSize, event);
		// TODO 1.14 Needed?
		payload.release();

		return event;
	}
}
