package openmods.network;

import io.netty.channel.embedded.EmbeddedChannel;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import openmods.network.senders.*;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;

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

		public final ITargetedPacketSender<EntityPlayer> player = FmlPacketSenderFactory.createPlayerSender(serverChannel());

		public final ITargetedPacketSender<Integer> dimension = FmlPacketSenderFactory.createDimensionSender(serverChannel());

		public final ITargetedPacketSender<TargetPoint> point = FmlPacketSenderFactory.createPointSender(serverChannel());

		public final ITargetedPacketSender<DimCoord> block = ExtPacketSenderFactory.createBlockSender(serverChannel());

		public final ITargetedPacketSender<Entity> entity = ExtPacketSenderFactory.createEntitySender(serverChannel());

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
