package openmods.network;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import openmods.Log;
import openmods.OpenMods;
import openmods.network.events.TileEntityMessageEventPacket;
import cpw.mods.fml.common.eventhandler.Event;

public abstract class EventPacket extends Event {

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
		};

		@Override
		public abstract EventPacket createPacket();

		@Override
		public abstract PacketDirection getDirection();

		@Override
		public boolean isCompressed() {
			return false;
		}

		@Override
		public boolean isChunked() {
			return false;
		}

		@Override
		public int getId() {
			return EventIdRanges.BASE_ID_START + ordinal();
		}
	}

	public static void registerCorePackets() {
		for (IEventPacketType type : CoreEventTypes.values())
			EventPacketManager.registerType(type);
	}

	public INetworkManager manager;

	public Player player;

	public abstract IEventPacketType getType();

	protected abstract void readFromStream(DataInput input) throws IOException;

	protected abstract void writeToStream(DataOutput output) throws IOException;

	protected void appendLogInfo(List<String> info) {}

	public void reply(EventPacket reply) {
		boolean isRemote = !(player instanceof EntityPlayerMP);
		if (!getType().getDirection().validateSend(isRemote)) {
			for (Packet packet : EventPacketManager.serializeEvent(reply))
				manager.addToSendQueue(packet);
		}
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
		if (checkSendToClient()) {
			for (Packet packet : EventPacketManager.serializeEvent(this))
				OpenMods.proxy.sendPacketToPlayer(player, packet);
		}
	}

	public void sendToPlayer(EntityPlayer player) {
		sendToPlayer((Player)player);
	}

	public void sendToPlayers(Collection<EntityPlayer> players) {
		if (checkSendToClient()) {
			for (Packet packet : EventPacketManager.serializeEvent(this))
				for (EntityPlayer player : players)
					OpenMods.proxy.sendPacketToPlayer((Player)player, packet);
		}
	}

	public void sendToServer() {
		if (checkSendToServer()) {
			for (Packet packet : EventPacketManager.serializeEvent(this))
				OpenMods.proxy.sendPacketToServer(packet);
		}
	}
}
