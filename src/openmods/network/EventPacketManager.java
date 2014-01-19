package openmods.network;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraftforge.common.MinecraftForge;
import openmods.LibConfig;
import openmods.utils.ByteUtils;
import openmods.utils.io.PacketChunker;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import cpw.mods.fml.common.network.Player;

public class EventPacketManager {
	private static final Map<Integer, IEventPacketType> TYPES = Maps.newHashMap();

	private static final PacketChunker CHUNKER = new PacketChunker();

	public static void registerType(IEventPacketType type) {
		final int typeId = type.getId();
		IEventPacketType prev = TYPES.put(typeId, type);
		Preconditions.checkState(prev == null, "Trying to re-register event type id %s with %s, prev %s", typeId, type, prev);
	}

	public static void handlePacket(Packet250CustomPayload packet, INetworkManager manager, Player player) {
		EventPacket event = deserializeEvent(packet, player, manager);
		if (event != null) MinecraftForge.EVENT_BUS.post(event);
	}

	private static EventPacket deserializeEvent(Packet250CustomPayload packet, Player player, INetworkManager manager) {
		try {
			InputStream input = new ByteArrayInputStream(packet.data);
			final IEventPacketType type = readType(input);

			if (type.isChunked()) {
				byte[] payload = IOUtils.toByteArray(input);
				byte[] fullPayload = CHUNKER.consumeChunk(payload);
				if (fullPayload == null) {
					if (LibConfig.logPackets) PacketLogger.log(packet, true, createUnfinishedLogInfo(type, player));
					return null;
				}
				input = new ByteArrayInputStream(fullPayload);
			}

			if (type.isCompressed()) input = new GZIPInputStream(input);

			DataInput data = new DataInputStream(input);

			EventPacket event = type.createPacket();
			event.readFromStream(data);
			input.close();

			event.manager = manager;
			event.player = player;

			if (LibConfig.logPackets) PacketLogger.log(packet, true, createLogInfo(event, 0, 0));
			return event;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	private static IEventPacketType readType(InputStream bytes) {
		DataInput input = new DataInputStream(bytes);
		int id = ByteUtils.readVLI(input);
		IEventPacketType type = TYPES.get(id);
		Preconditions.checkNotNull(type, "Unknown type id: %s", id);
		return type;
	}

	private static Packet250CustomPayload createEventPacket(IEventPacketType type, byte[] bytes) {
		ByteArrayDataOutput output = ByteStreams.newDataOutput();
		ByteUtils.writeVLI(output, type.getId());
		output.write(bytes);
		return new Packet250CustomPayload(PacketHandler.CHANNEL_EVENTS, output.toByteArray());
	}

	private static byte[] serializeToBytes(EventPacket event) throws IOException {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();

		OutputStream stream = event.getType().isCompressed()? new GZIPOutputStream(payload) : payload;
		DataOutputStream output = new DataOutputStream(stream);
		event.writeToStream(output);
		stream.close();

		return payload.toByteArray();
	}

	static List<Packet250CustomPayload> serializeEvent(EventPacket event) {
		try {
			final IEventPacketType type = event.getType();

			byte[] bytes = serializeToBytes(event);

			if (type.isChunked()) {
				ImmutableList.Builder<Packet250CustomPayload> builder = ImmutableList.builder();
				byte[][] chunked = CHUNKER.splitIntoChunks(bytes);
				for (int chunkIndex = 0; chunkIndex < chunked.length; chunkIndex++) {
					Packet250CustomPayload result = createEventPacket(type, chunked[chunkIndex]);
					if (LibConfig.logPackets) PacketLogger.log(result, false, createLogInfo(event, chunkIndex + 1, chunked.length));
					builder.add(result);
				}
				return builder.build();
			} else {
				Packet250CustomPayload result = createEventPacket(type, bytes);
				if (LibConfig.logPackets) PacketLogger.log(result, false, createLogInfo(event, 0, 0));
				return ImmutableList.of(result);
			}
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	private static List<String> createLogInfo(EventPacket event, int chunkId, int chunkLength) {
		List<String> info = Lists.newArrayList();
		info.add(String.format("%d/%d", chunkId, chunkLength));
		addTypeInfo(info, event.getType());
		addPlayerInfo(info, event.player);
		event.appendLogInfo(info);
		return info;
	}

	private static List<String> createUnfinishedLogInfo(IEventPacketType type, Player player) {
		List<String> info = Lists.newArrayList();
		addTypeInfo(info, type);
		info.add("?/?");
		addPlayerInfo(info, player);
		info.add("non-final chunk");
		return info;
	}

	private static void addTypeInfo(List<String> info, final IEventPacketType type) {
		info.add(Integer.toString(type.getId()));
		info.add(type.toString());
		info.add(type.isCompressed()? "packed" : "raw");
		info.add(type.isChunked()? "chunked" : "single");
	}

	private static void addPlayerInfo(List<String> info, Player player) {
		info.add(ObjectUtils.toString(player));
	}
}
