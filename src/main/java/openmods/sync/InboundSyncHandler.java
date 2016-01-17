package openmods.sync;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import openmods.OpenMods;
import openmods.utils.NetUtils;

@Sharable
public class InboundSyncHandler extends SimpleChannelInboundHandler<FMLProxyPacket> {

	public static class SyncException extends RuntimeException {
		private static final long serialVersionUID = 2585053869917082095L;

		public SyncException(Throwable cause, ISyncMapProvider provider) {
			super(String.format("Failed to sync %s (%s)", provider, provider.getClass()), cause);
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, final FMLProxyPacket msg) throws Exception {
		NetUtils.executeSynchronized(ctx, new Runnable() {
			@Override
			public void run() {
				World world = OpenMods.proxy.getClientWorld();

				PacketBuffer payload = new PacketBuffer(msg.payload());

				ISyncMapProvider provider = SyncMap.findSyncMap(world, payload);
				try {
					if (provider != null) provider.getSyncMap().readFromStream(payload);
				} catch (Throwable e) {
					throw new SyncException(e, provider);
				}
			}
		});
	}
}
