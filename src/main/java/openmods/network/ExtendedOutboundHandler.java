package openmods.network;

import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;

public class ExtendedOutboundHandler extends ChannelOutboundHandlerAdapter {
	public static final AttributeKey<IPacketTargetSelector> MESSAGETARGET = new AttributeKey<IPacketTargetSelector>("om:outboundTarget");

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (!(msg instanceof FMLProxyPacket)) {
			ctx.write(msg);
			return;
		}

		final Channel channel = ctx.channel();

		final IPacketTargetSelector target = channel.attr(MESSAGETARGET).get();
		if (target == null) {
			ctx.write(msg);
			return;
		}

		final FMLProxyPacket pkt = (FMLProxyPacket)msg;

		final Side channelSide = channel.attr(NetworkRegistry.CHANNEL_SOURCE).get();

		Preconditions.checkState(target.isAllowedOnSide(channelSide), "Packet not allowed on side");

		final String channelName = channel.attr(NetworkRegistry.FML_CHANNEL).get();

		Object arg = channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).get();

		try {
			Collection<NetworkDispatcher> dispatchers = Lists.newArrayList();
			target.listDispatchers(arg, dispatchers);

			for (NetworkDispatcher dispatcher : dispatchers)
				dispatcher.sendProxy(pkt);

		} catch (Throwable t) {

			throw new IllegalStateException(String.format(
					"Failed to select and send message (selector %s, arg: %s, channel: %s, side: %s)",
					target, arg, channelName, channelSide), t);
		}

	}

	public static void install(Map<Side, FMLEmbeddedChannel> channels) {
		for (Side side : Side.values())
			install(channels.get(side));
	}

	public static void install(FMLEmbeddedChannel fmlEmbeddedChannel) {
		fmlEmbeddedChannel.pipeline().addAfter("fml:outbound", "om:outbound", new ExtendedOutboundHandler());
	}

	public static void selectTargets(EmbeddedChannel channel, IPacketTargetSelector selector, Object arg) {
		channel.attr(MESSAGETARGET).set(selector);
		if (arg != null) channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(arg);
	}

}
