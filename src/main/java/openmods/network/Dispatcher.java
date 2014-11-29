package openmods.network;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import openmods.network.senders.*;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;

public abstract class Dispatcher<M> {

	protected abstract EmbeddedChannel getChannel(Side side);

	protected EmbeddedChannel serverChannel() {
		return getChannel(Side.SERVER);
	}

	protected EmbeddedChannel clientChannel() {
		return getChannel(Side.CLIENT);
	}

	public class Senders {
		public final IPacketSender<M> client = FmlPacketSenderFactory.createClientSender(clientChannel());

		public final IPacketSender<M> global = FmlPacketSenderFactory.createGlobalSender(serverChannel());

		public final ITargetedPacketSender<M, EntityPlayer> player = FmlPacketSenderFactory.createPlayerSender(serverChannel());

		public final ITargetedPacketSender<M, Integer> dimension = FmlPacketSenderFactory.createDimensionSender(serverChannel());

		public final ITargetedPacketSender<M, TargetPoint> point = FmlPacketSenderFactory.createPointSender(serverChannel());

		public final ITargetedPacketSender<M, DimCoord> block = ExtPacketSenderFactory.createBlockSender(serverChannel());

		public final ITargetedPacketSender<M, Entity> entity = ExtPacketSenderFactory.createEntitySender(serverChannel());
	}
}
