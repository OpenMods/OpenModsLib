package openmods.sync;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.senders.ExtPacketSenderFactory;
import openmods.network.senders.ITargetedPacketSender;

public class SyncChannelHolder {

	public static final String CHANNEL_NAME = "OpenMods|M";

	public static final SyncChannelHolder INSTANCE = new SyncChannelHolder();

	private final Map<Side, ITargetedPacketSender<Collection<ServerPlayerEntity>>> senders = Maps.newEnumMap(Side.class);

	private SyncChannelHolder() {
		final EnumMap<Side, FMLEmbeddedChannel> channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new InboundSyncHandler());

		for (Map.Entry<Side, FMLEmbeddedChannel> e : channels.entrySet()) {
			final FMLEmbeddedChannel channel = e.getValue();
			ExtendedOutboundHandler.install(channel);
			senders.put(e.getKey(), ExtPacketSenderFactory.createMultiplePlayersSender(channel));
		}
	}

	public static IPacket<?> createPacket(PacketBuffer payload) {
		return new FMLProxyPacket(payload, CHANNEL_NAME);
	}

	public void sendPayloadToPlayers(PacketBuffer payload, Collection<ServerPlayerEntity> players) {
		FMLProxyPacket packet = new FMLProxyPacket(payload, CHANNEL_NAME);
		senders.get(Side.SERVER).sendMessage(packet, players);
	}

	public static void ensureLoaded() {}
}
