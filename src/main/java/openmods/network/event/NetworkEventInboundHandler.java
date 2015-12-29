package openmods.network.event;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.FMLOutboundHandler.OutboundTarget;

@Sharable
public class NetworkEventInboundHandler extends SimpleChannelInboundHandler<NetworkEvent> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, NetworkEvent msg) throws Exception {
		MinecraftForge.EVENT_BUS.post(msg);
		msg.dispatcher = null;

		for (NetworkEvent reply : msg.replies) {
			ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGET).set(OutboundTarget.REPLY);
			ctx.writeAndFlush(reply);
		}
	}

}
