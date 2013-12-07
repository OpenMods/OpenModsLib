package openmods.network;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraftforge.event.Event;
import openmods.LibConfig;
import openmods.Log;
import openmods.OpenMods;
import openmods.network.events.TileEntityMessageEventPacket;
import openmods.utils.ByteUtils;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.network.Player;

public abstract class EventPacket extends Event {
	private static final Map<Integer, IEventPacketType> TYPES = Maps.newHashMap();

	public enum CoreEventTypes implements IEventPacketType {
		TILE_ENTITY_NOTIFY {

			@Override
			public EventPacket createPacket() {
				return new TileEntityMessageEventPacket();
			}

			@Override
			public PacketDirection getDirection() {
				return PacketDirection.ANY;
			}

			@Override
			public int getId() {
				return EventIdRanges.BASE_ID_START + ordinal();
			}
		};

		@Override
		public abstract EventPacket createPacket();

		@Override
		public abstract PacketDirection getDirection();

		@Override
		public boolean isCompressed() {
			return false;
		}
	}

	public static void registerType(IEventPacketType type) {
		final int typeId = type.getId();
		IEventPacketType prev = TYPES.put(typeId, type);
		Preconditions.checkState(prev == null, "Trying to re-register event type id %s with %s, prev %s", typeId, type, prev);
	}

	public static void regiterCorePackets() {
		for (IEventPacketType type : CoreEventTypes.values())
			registerType(type);
	}

	public static EventPacket deserializeEvent(Packet250CustomPayload packet) {
		try {
			ByteArrayInputStream bytes = new ByteArrayInputStream(packet.data);

			EventPacket event;
			IEventPacketType type;
			{
				DataInput input = new DataInputStream(bytes);
				int id = ByteUtils.readVLI(input);
				type = TYPES.get(id);
				event = type.createPacket();
			}

			InputStream stream = type.isCompressed()? new GZIPInputStream(bytes) : bytes;

			DataInput input = new DataInputStream(stream);
			event.readFromStream(input);
			stream.close();

			return event;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static Packet250CustomPayload serializeEvent(EventPacket event) {
		try {
			ByteArrayOutputStream payload = new ByteArrayOutputStream();

			IEventPacketType type = event.getType();
			{
				DataOutput output = new DataOutputStream(payload);
				ByteUtils.writeVLI(output, type.getId());
			}

			OutputStream stream = type.isCompressed()? new GZIPOutputStream(payload) : payload;

			DataOutputStream output = new DataOutputStream(stream);
			event.writeToStream(output);
			stream.close();

			Packet250CustomPayload result = new Packet250CustomPayload(PacketHandler.CHANNEL_EVENTS, payload.toByteArray());

			if (LibConfig.logPackets) PacketLogger.log(result, false, createLogInfo(event));

			return result;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static List<String> createLogInfo(EventPacket event) {
		List<String> info = Lists.newArrayList();
		final IEventPacketType type = event.getType();
		info.add(Integer.toString(type.getId()));
		info.add(type.toString());
		info.add(type.isCompressed()? "packed" : "raw");
		info.add(ObjectUtils.toString(event.player));
		event.appendLogInfo(info);
		return info;
	}

	public INetworkManager manager;

	public Player player;

	public abstract IEventPacketType getType();

	protected abstract void readFromStream(DataInput input) throws IOException;

	protected abstract void writeToStream(DataOutput output) throws IOException;

	protected void appendLogInfo(List<String> info) {}

	public void reply(EventPacket reply) {
		boolean isRemote = !(player instanceof EntityPlayerMP);
		if (!getType().getDirection().validateSend(isRemote)) manager.addToSendQueue(serializeEvent(reply));
		else Log.warn("Invalid sent direction for packet '%s'", this);
	}

	protected boolean checkSendToClient() {
		if (!getType().getDirection().toClient) {
			Log.warn("Trying to sent message '%s' to client", this);
			return false;
		}
		return true;
	}

	protected boolean checkSendToServer() {
		if (!getType().getDirection().toServer) {
			Log.warn("Trying to sent message '%s' to server", this);
			return false;
		}
		return true;
	}

	public void sendToPlayer(Player player) {
		if (checkSendToClient()) OpenMods.proxy.sendPacketToPlayer(player, serializeEvent(this));
	}

	public void sendToServer() {
		if (checkSendToServer()) OpenMods.proxy.sendPacketToServer(serializeEvent(this));
	}
}
