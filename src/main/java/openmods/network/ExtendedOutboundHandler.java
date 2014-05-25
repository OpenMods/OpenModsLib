package openmods.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
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

		IPacketTargetSelector target = ctx.channel().attr(MESSAGETARGET).get();
		if (target == null) {
			ctx.write(msg);
			return;
		}

		Side channelSide = ctx.channel().attr(NetworkRegistry.CHANNEL_SOURCE).get();

		Preconditions.checkState(target.isAllowedOnSide(channelSide), "Packet not allowed on side");

		Object arg = ctx.channel().attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).get();

		Collection<NetworkDispatcher> dispatchers = Lists.newArrayList();
		target.listDispatchers(arg, dispatchers);

		FMLProxyPacket pkt = (FMLProxyPacket)msg;

		for (NetworkDispatcher dispatcher : dispatchers)
			dispatcher.sendProxy(pkt);
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
