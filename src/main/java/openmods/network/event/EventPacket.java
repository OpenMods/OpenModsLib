package openmods.network.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import com.google.common.collect.Lists;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;

public abstract class EventPacket extends Event {

	final List<EventPacket> replies = Lists.newArrayList();

	NetworkDispatcher dispatcher;

	protected abstract void readFromStream(DataInput input) throws IOException;

	protected abstract void writeToStream(DataOutput output) throws IOException;

	protected void appendLogInfo(List<String> info) {}

	protected boolean checkSendToClient() {
		// if (!getType().getDirection().toClient) {
		// Log.warn("Trying to sent message '%s' to client", this);
		// return false;
		// }
		return true;
	}

	protected boolean checkSendToServer() {
		// if (!getType().getDirection().toServer) {
		// Log.warn("Trying to sent message '%s' to server", this);
		// return false;
		// }
		return true;
	}

	public void reply(EventPacket reply) {
		reply.dispatcher = dispatcher;
		this.replies.add(reply);
	}

	public void sendToPlayer(EntityPlayer player) {
		// sendToPlayer((Player)player);
	}

	public void sendToPlayers(Collection<EntityPlayer> players) {
		// if (checkSendToClient()) {
		// for (Packet packet : EventPacketManager.serializeEvent(this))
		// for (EntityPlayer player : players)
		// OpenMods.proxy.sendPacketToPlayer((Player)player, packet);
		// }
	}

	public void sendToServer() {
		// if (checkSendToServer()) {
		// for (Packet packet : EventPacketManager.serializeEvent(this))
		// OpenMods.proxy.sendPacketToServer(packet);
		// }
	}
}
