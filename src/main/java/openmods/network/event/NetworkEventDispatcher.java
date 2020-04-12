package openmods.network.event;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.ICustomPacket;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.registries.IForgeRegistry;
import openmods.OpenMods;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkEventDispatcher {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final ResourceLocation CHANNEL_ID = OpenMods.location("events");

	private static final String PROTOCOL_VERSION = Integer.toString(1);

	private final NetworkEventCodec codec;

	public NetworkEventDispatcher(IForgeRegistry<NetworkEventEntry> registry) {
		this.codec = new NetworkEventCodec(registry);
		EventNetworkChannel channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_ID)
				.clientAcceptedVersions(PROTOCOL_VERSION::equals)
				.serverAcceptedVersions(PROTOCOL_VERSION::equals)
				.networkProtocolVersion(() -> PROTOCOL_VERSION)
				.eventNetworkChannel();

		channel.addListener(evt -> {
			if (evt instanceof net.minecraftforge.fml.network.NetworkEvent.LoginPayloadEvent || evt instanceof net.minecraftforge.fml.network.NetworkEvent.ChannelRegistrationChangeEvent) {
				return;
			}

			final net.minecraftforge.fml.network.NetworkEvent.Context context = evt.getSource().get();
			try {
				final NetworkEvent event = codec.decode(evt.getPayload(), context);
				MinecraftForge.EVENT_BUS.post(event);
			} catch (final IOException e) {
				LOGGER.error("Failed to receive message: {}", e);
			}
			context.setPacketHandled(true);
		});
	}

	public void send(final NetworkEvent event, final PacketDistributor.PacketTarget target) {
		send(event, target.getDirection(), target::send);
	}

	public void send(final NetworkEvent event, final NetworkDirection direction, Consumer<IPacket<?>> output) {
		try {
			final PacketBuffer payload = codec.encode(event, direction.getOriginationSide());
			final ICustomPacket<IPacket<?>> packet = direction.buildPacket(Pair.of(payload, 0), CHANNEL_ID);
			output.accept(packet.getThis());
		} catch (final IOException e) {
			LOGGER.error("Failed to send message: {}", e);
		}
	}

	private static Consumer<IPacket<?>> trackingChunk(final PacketDistributor<?> distributor, final Supplier<Pair<IWorld, ChunkPos>> chunkPosSupplier) {
		return p -> {
			final Pair<IWorld, ChunkPos> info = chunkPosSupplier.get();
			((ServerChunkProvider)info.getKey().getChunkProvider()).chunkManager.getTrackingPlayers(info.getRight(), false).forEach(e -> e.connection.sendPacket(p));
		};
	}

	public static final PacketDistributor<Pair<IWorld, ChunkPos>> TRACKING_CHUNK = new PacketDistributor<>(NetworkEventDispatcher::trackingChunk, NetworkDirection.PLAY_TO_CLIENT);
}
