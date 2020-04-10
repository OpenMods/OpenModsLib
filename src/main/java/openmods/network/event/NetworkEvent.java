package openmods.network.event;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.network.PacketDistributor;

public abstract class NetworkEvent extends Event {
	public PlayerEntity sender;

	public net.minecraftforge.fml.network.NetworkEvent.Context context;

	protected abstract void readFromStream(PacketBuffer input) throws IOException;

	protected abstract void writeToStream(PacketBuffer output) throws IOException;

	public void reply(NetworkEvent reply) {
		// TODO 1.14 Revise (problems with index)
	}

	public void sendToAll() {
		NetworkEventManager.dispatcher().send(this, PacketDistributor.ALL.noArg());
	}

	public void sendToServer() {
		NetworkEventManager.dispatcher().send(this, PacketDistributor.SERVER.noArg());
	}

	public void sendToPlayer(ServerPlayerEntity player) {
		NetworkEventManager.dispatcher().send(this, PacketDistributor.PLAYER.with(() -> player));
	}

	public void sendToEntity(Entity entity) {
		NetworkEventManager.dispatcher().send(this, PacketDistributor.TRACKING_ENTITY.with(() -> entity));
	}
}
