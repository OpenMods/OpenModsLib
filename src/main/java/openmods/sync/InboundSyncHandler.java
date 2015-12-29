package openmods.sync;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.DataInputStream;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import openmods.OpenMods;

@Sharable
public class InboundSyncHandler extends SimpleChannelInboundHandler<FMLProxyPacket> {

	public static class SyncException extends RuntimeException {
		private static final long serialVersionUID = 2585053869917082095L;

		public SyncException(Throwable cause, ISyncMapProvider provider) {
			super(String.format("Failed to sync %s (%s)", provider, provider.getClass()), cause);
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FMLProxyPacket msg) throws Exception {
		World world = OpenMods.proxy.getClientWorld();

		ByteBuf payload = msg.payload();
		DataInputStream input = new DataInputStream(new ByteBufInputStream(payload));

		ISyncMapProvider provider = SyncMap.findSyncMap(world, input);
		try {
			if (provider != null) provider.getSyncMap().readFromStream(input);
		} catch (Throwable e) {
			throw new SyncException(e, provider);
		}
	}
}
