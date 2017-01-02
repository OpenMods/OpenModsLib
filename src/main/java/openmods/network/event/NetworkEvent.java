package openmods.network.event;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

public abstract class NetworkEvent extends Event {

	final List<NetworkEvent> replies = Lists.newArrayList();

	NetworkDispatcher dispatcher;

	public EntityPlayer sender;

	protected abstract void readFromStream(PacketBuffer input) throws IOException;

	protected abstract void writeToStream(PacketBuffer output) throws IOException;

	protected void appendLogInfo(List<String> info) {}

	public void reply(NetworkEvent reply) {
		Preconditions.checkState(dispatcher != null, "Can't call this method outside event handler");
		reply.dispatcher = dispatcher;
		this.replies.add(reply);
	}

	public void sendToAll() {
		NetworkEventManager.INSTANCE.dispatcher().senders.global.sendMessage(this);
	}

	public void sendToServer() {
		NetworkEventManager.INSTANCE.dispatcher().senders.client.sendMessage(this);
	}

	public void sendToPlayer(EntityPlayer player) {
		NetworkEventManager.INSTANCE.dispatcher().senders.player.sendMessage(this, player);
	}

	public void sendToEntity(Entity entity) {
		NetworkEventManager.INSTANCE.dispatcher().senders.entity.sendMessage(this, entity);
	}

	public List<Object> serialize() {
		return NetworkEventManager.INSTANCE.dispatcher().senders.serialize(this);
	}
}
