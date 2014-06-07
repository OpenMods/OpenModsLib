package openmods.network.event;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;

@Sharable
public class EventPacketInboundHandler extends SimpleChannelInboundHandler<EventPacket> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, EventPacket msg) throws Exception {
		MinecraftForge.EVENT_BUS.post(msg);
		msg.dispatcher = null;

		for (EventPacket reply : msg.replies) {
			ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.REPLY);
			ctx.writeAndFlush(reply);
		}
	}

}
