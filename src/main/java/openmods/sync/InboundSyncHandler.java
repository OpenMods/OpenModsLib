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
				PacketBuffer payload = new PacketBuffer(msg.payload());

				final ISyncMapProvider provider = findSyncMapProvider(payload);

				try {
					if (provider != null) provider.getSyncMap().readUpdate(payload);
				} catch (Throwable e) {
					throw new SyncException(e, provider);
				}
			}

			private ISyncMapProvider findSyncMapProvider(PacketBuffer payload) {
				final int ownerType = payload.readVarIntFromBuffer();

				final World world = OpenMods.proxy.getClientWorld();

				switch (ownerType) {
					case SyncMapEntity.OWNER_TYPE:
						return SyncMapEntity.findOwner(world, payload);
					case SyncMapTile.OWNER_TYPE:
						return SyncMapTile.findOwner(world, payload);
					default:
						throw new IllegalArgumentException("Unknown sync map owner type: " + ownerType);
				}

			}
		});
	}
}
