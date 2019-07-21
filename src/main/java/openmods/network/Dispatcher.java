package openmods.network;

import com.google.common.collect.ImmutableList;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.common.network.FMLOutboundHandler.OutboundTarget;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import openmods.network.senders.FmlPacketSenderFactory;
import openmods.network.senders.IPacketSender;
import openmods.network.senders.ITargetedPacketSender;

public abstract class Dispatcher {

	protected abstract EmbeddedChannel getChannel(Side side);

	protected EmbeddedChannel serverChannel() {
		return getChannel(Side.SERVER);
	}

	protected EmbeddedChannel clientChannel() {
		return getChannel(Side.CLIENT);
	}

	public class Senders {
		public final IPacketSender client = FmlPacketSenderFactory.createSender(clientChannel(), OutboundTarget.TOSERVER);

		public final IPacketSender global = FmlPacketSenderFactory.createSender(serverChannel(), OutboundTarget.ALL);

		public final IPacketSender nowhere = FmlPacketSenderFactory.createSender(serverChannel(), OutboundTarget.NOWHERE);

		public final ITargetedPacketSender<PlayerEntity> player = FmlPacketSenderFactory.createPlayerSender(serverChannel());

		public final ITargetedPacketSender<Integer> dimension = FmlPacketSenderFactory.createDimensionSender(serverChannel());

		public final ITargetedPacketSender<TargetPoint> point = FmlPacketSenderFactory.createPointSender(serverChannel());

		public final ITargetedPacketSender<TargetPoint> block = FmlPacketSenderFactory.createBlockTrackersSender(serverChannel());

		public final ITargetedPacketSender<Entity> entity = FmlPacketSenderFactory.createEntityTrackersSender(serverChannel());

		public List<Object> serialize(Object msg) {
			nowhere.sendMessage(msg);

			ImmutableList.Builder<Object> result = ImmutableList.builder();
			Object packet;
			while ((packet = serverChannel().outboundMessages().poll()) != null)
				result.add(packet);

			return result.build();
		}
	}
}
