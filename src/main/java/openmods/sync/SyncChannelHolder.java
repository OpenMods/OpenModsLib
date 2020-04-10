package openmods.sync;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.ICustomPacket;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import openmods.OpenMods;
import org.apache.commons.lang3.tuple.Pair;

public class SyncChannelHolder {

	private static final ResourceLocation CHANNEL_ID = OpenMods.location("sync");
	private static final String PROTOCOL_VERSION = Integer.toString(1);

	public static class SyncException extends RuntimeException {
		private static final long serialVersionUID = 2585053869917082095L;

		public SyncException(Throwable cause, ISyncMapProvider provider) {
			super(String.format("Failed to sync %s (%s)", provider, provider.getClass()), cause);
		}
	}

	private ISyncMapProvider findSyncMapProvider(PacketBuffer payload) {
		final int ownerType = payload.readVarInt();

		final World world = OpenMods.PROXY.getClientWorld();

		switch (ownerType) {
			case SyncMapEntity.OWNER_TYPE:
				return SyncMapEntity.findOwner(world, payload);
			case SyncMapTile.OWNER_TYPE:
				return SyncMapTile.findOwner(world, payload);
			default:
				throw new IllegalArgumentException("Unknown sync map owner type: " + ownerType);
		}
	}

	private void handle(PacketBuffer payload, Supplier<NetworkEvent.Context> source) {
		final ISyncMapProvider provider = findSyncMapProvider(payload);
		try {
			if (provider != null) { provider.getSyncMap().readUpdate(payload); }
		} catch (Throwable e) {
			throw new SyncException(e, provider);
		}
	}

	public static final SyncChannelHolder INSTANCE = new SyncChannelHolder();

	private SyncChannelHolder() {
		EventNetworkChannel channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_ID)
				.clientAcceptedVersions(PROTOCOL_VERSION::equals)
				.serverAcceptedVersions(PROTOCOL_VERSION::equals)
				.networkProtocolVersion(() -> PROTOCOL_VERSION)
				.eventNetworkChannel();

		channel.addListener((NetworkEvent.ClientCustomPayloadEvent evt) -> handle(evt.getPayload(), evt.getSource()));
	}

	void sendPayload(PacketBuffer payload, final Collection<ServerPlayerEntity> players) {
		final List<NetworkManager> managers = players.stream().map(p -> p.connection.netManager).collect(Collectors.toList());
		final PacketDistributor.PacketTarget target = PacketDistributor.NMLIST.with(() -> managers);
		final ICustomPacket<IPacket<?>> packet = target.getDirection().buildPacket(Pair.of(payload, 0), CHANNEL_ID);
		target.send(packet.getThis());
	}

	public static void ensureLoaded() {}
}
