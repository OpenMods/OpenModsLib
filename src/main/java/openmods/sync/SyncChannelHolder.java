package openmods.sync;

import com.google.common.collect.Maps;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.senders.ExtPacketSenderFactory;
import openmods.network.senders.ITargetedPacketSender;

public class SyncChannelHolder {

	public static final String CHANNEL_NAME = "OpenMods|M";

	public static final SyncChannelHolder INSTANCE = new SyncChannelHolder();

	private final Map<Side, ITargetedPacketSender<Collection<EntityPlayerMP>>> senders = Maps.newEnumMap(Side.class);

	private SyncChannelHolder() {
		final EnumMap<Side, FMLEmbeddedChannel> channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new InboundSyncHandler());

		for (Map.Entry<Side, FMLEmbeddedChannel> e : channels.entrySet()) {
			final FMLEmbeddedChannel channel = e.getValue();
			ExtendedOutboundHandler.install(channel);
			senders.put(e.getKey(), ExtPacketSenderFactory.createMultiplePlayersSender(channel));
		}
	}

	public static Packet createPacket(ByteBuf payload) {
		return new FMLProxyPacket(payload, CHANNEL_NAME);
	}

	public void sendPayloadToPlayers(ByteBuf payload, Collection<EntityPlayerMP> players) {
		FMLProxyPacket packet = new FMLProxyPacket(payload, CHANNEL_NAME);
		senders.get(Side.SERVER).sendMessage(packet, players);
	}

	public static void ensureLoaded() {}
}
