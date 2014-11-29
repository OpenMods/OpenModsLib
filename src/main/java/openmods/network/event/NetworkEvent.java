package openmods.network.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;

public abstract class NetworkEvent extends Event {

	final List<NetworkEvent> replies = Lists.newArrayList();

	NetworkDispatcher dispatcher;

	public EntityPlayer sender;

	protected abstract void readFromStream(DataInput input) throws IOException;

	protected abstract void writeToStream(DataOutput output) throws IOException;

	protected void appendLogInfo(List<String> info) {}

	public void reply(NetworkEvent reply) {
		Preconditions.checkState(dispatcher != null, "Can't call this method outside event handler");
		reply.dispatcher = dispatcher;
		this.replies.add(reply);
	}

	public void sendToAll() {
		NetworkEventManager.INSTANCE.dispatcher().senders.global.sendPacket(this);
	}

	public void sendToServer() {
		NetworkEventManager.INSTANCE.dispatcher().senders.client.sendPacket(this);
	}

	public void sendToPlayer(EntityPlayer player) {
		NetworkEventManager.INSTANCE.dispatcher().senders.player.sendPacket(this, player);
	}
}
