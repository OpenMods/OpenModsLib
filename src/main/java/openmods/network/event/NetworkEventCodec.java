package openmods.network.event;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.io.IOException;
import java.util.List;

import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import openmods.Log;
import openmods.OpenMods;

import com.google.common.base.Preconditions;

// TODO compression
@Sharable
public class NetworkEventCodec extends MessageToMessageCodec<FMLProxyPacket, NetworkEvent> {

	private final NetworkEventRegistry registry;

	public NetworkEventCodec(NetworkEventRegistry registry) {
		this.registry = registry;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, NetworkEvent msg, List<Object> out) throws IOException {
		final Channel channel = ctx.channel();
		final Side side = channel.attr(NetworkRegistry.CHANNEL_SOURCE).get();

		final int id = registry.getIdForClass(msg.getClass());
		final INetworkEventType type = registry.getTypeForId(id);

		final EventDirection validator = type.getDirection();
		Preconditions.checkState(validator != null && validator.validateSend(side),
				"Invalid direction: sending packet %s on side %s", msg.getClass(), side);

		final PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
		buf.writeVarIntToBuffer(id);
		msg.writeToStream(buf);

		final FMLProxyPacket packet = new FMLProxyPacket(buf, NetworkEventDispatcher.CHANNEL_NAME);
		packet.setDispatcher(msg.dispatcher);
		out.add(packet);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
		final Channel channel = ctx.channel();
		final Side side = channel.attr(NetworkRegistry.CHANNEL_SOURCE).get();

		final PacketBuffer payload = new PacketBuffer(msg.payload());
		final int typeId = payload.readVarIntFromBuffer();
		final INetworkEventType type = registry.getTypeForId(typeId);

		final EventDirection validator = type.getDirection();
		Preconditions.checkState(validator != null && validator.validateReceive(side),
				"Invalid direction: receiving packet %s on side %s", msg.getClass(), side);

		final NetworkEvent event = type.createPacket();
		event.readFromStream(payload);
		event.dispatcher = msg.getDispatcher();

		final INetHandler handler = msg.handler();
		if (handler != null) event.sender = OpenMods.proxy.getPlayerFromHandler(handler);

		final int bufferJunkSize = payload.readableBytes();
		if (bufferJunkSize > 0) Log.warn("%s junk bytes left in buffer, event %s", bufferJunkSize, event);

		out.add(event);
	}
}
