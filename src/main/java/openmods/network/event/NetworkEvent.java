package openmods.network.event;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.relauncher.Side;

public abstract class NetworkEvent extends Event {

	final List<NetworkEvent> replies = Lists.newArrayList();

	NetworkDispatcher dispatcher;

	public PlayerEntity sender;

	public Side side;

	protected abstract void readFromStream(PacketBuffer input) throws IOException;

	protected abstract void writeToStream(PacketBuffer output) throws IOException;

	protected void appendLogInfo(List<String> info) {}

	public void reply(NetworkEvent reply) {
		Preconditions.checkState(dispatcher != null, "Can't call this method outside event handler");
		reply.dispatcher = dispatcher;
		this.replies.add(reply);
	}

	public void sendToAll() {
		NetworkEventManager.dispatcher().senders.global.sendMessage(this);
	}

	public void sendToServer() {
		NetworkEventManager.dispatcher().senders.client.sendMessage(this);
	}

	public void sendToPlayer(PlayerEntity player) {
		NetworkEventManager.dispatcher().senders.player.sendMessage(this, player);
	}

	public void sendToEntity(Entity entity) {
		NetworkEventManager.dispatcher().senders.entity.sendMessage(this, entity);
	}

	public List<Object> serialize() {
		return NetworkEventManager.dispatcher().senders.serialize(this);
	}
}
