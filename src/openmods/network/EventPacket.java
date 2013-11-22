package openmods.network;

import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraftforge.event.Event;
import openmods.Log;
import openmods.OpenMods;
import openmods.network.events.TileEntityMessageEventPacket;
import openmods.utils.ByteUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
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

	public static EventPacket deserializeEvent(Packet250CustomPayload packet) throws IOException {
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

		{
			DataInput input = new DataInputStream(stream);
			event.readFromStream(input);
		}

		stream.close();
		return event;
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

			{
				DataOutput output = new DataOutputStream(stream);
				event.writeToStream(output);
				stream.close();
			}

			Packet250CustomPayload result = new Packet250CustomPayload(PacketHandler.CHANNEL_EVENTS, payload.toByteArray());
			return result;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public INetworkManager manager;

	public Player player;

	public abstract IEventPacketType getType();

	protected abstract void readFromStream(DataInput input) throws IOException;

	protected abstract void writeToStream(DataOutput output) throws IOException;

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
