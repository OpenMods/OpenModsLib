package openmods.network.senders;

import io.netty.channel.Channel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.FMLOutboundHandler.OutboundTarget;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class FmlPacketSenderFactory {

	public static ITargetedPacketSender<EntityPlayer> createPlayerSender(Channel channel) {
		return new FmlTargetedPacketSender<EntityPlayer>(channel, OutboundTarget.PLAYER);
	}

	public static ITargetedPacketSender<Integer> createDimensionSender(Channel channel) {
		return new FmlTargetedPacketSender<Integer>(channel, OutboundTarget.DIMENSION);
	}

	public static ITargetedPacketSender<TargetPoint> createPointSender(Channel channel) {
		return new FmlTargetedPacketSender<TargetPoint>(channel, OutboundTarget.ALLAROUNDPOINT);
	}

	public static IPacketSender createSender(Channel channel, OutboundTarget target) {
		return new FmlPacketSender(channel, target);
	}

	private static class FmlPacketSender extends PacketSenderBase {
		private final OutboundTarget selector;

		public FmlPacketSender(Channel channel, OutboundTarget selector) {
			super(channel);
			this.selector = selector;
		}

		@Override
		protected void configureChannel(Channel channel) {
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(selector);
		}
	}

	private static class FmlTargetedPacketSender<T> extends TargetedPacketSenderBase<T> {

		private final OutboundTarget selector;

		public FmlTargetedPacketSender(Channel channel, OutboundTarget selector) {
			super(channel);
			this.selector = selector;
		}

		@Override
		protected void configureChannel(Channel channel, T target) {
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(selector);
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(target);
		}
	}

}
