package openmods.sync;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.world.World;
import openmods.OpenMods;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

@Sharable
public class InboundSyncHandler extends SimpleChannelInboundHandler<FMLProxyPacket> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FMLProxyPacket msg) throws Exception {
		World world = OpenMods.proxy.getClientWorld();

		ByteBuf payload = msg.payload();
		ByteBufInputStream input = new ByteBufInputStream(payload);

		ISyncProvider provider = SyncMap.findSyncMap(world, input);
		if (provider != null) provider.getSyncMap().readFromStream(input);
	}
}
