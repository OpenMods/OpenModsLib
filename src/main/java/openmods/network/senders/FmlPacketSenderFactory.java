package openmods.network.senders;

import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import io.netty.channel.Channel;
import net.minecraft.entity.player.EntityPlayer;

public class FmlPacketSenderFactory {

	public static ITargetedPacketSender<EntityPlayer> createPlayerSender(Channel channel) {
		return new FmlTargetedPacketSender<EntityPlayer>(channel, OutboundTarget.PLAYER) {
			@Override
			protected void configureChannel(Channel channel, EntityPlayer player) {
				super.configureChannel(channel, player);
				setTargetAttr(channel, player);
			}
		};
	}

	public static ITargetedPacketSender<Integer> createDimensionSender(Channel channel) {
		return new FmlTargetedPacketSender<Integer>(channel, OutboundTarget.DIMENSION) {
			@Override
			protected void configureChannel(Channel channel, Integer dimensionId) {
				super.configureChannel(channel, dimensionId);
				setTargetAttr(channel, dimensionId);
			}
		};
	}

	public static ITargetedPacketSender<TargetPoint> createPointSender(Channel channel) {
		return new FmlTargetedPacketSender<TargetPoint>(channel, OutboundTarget.ALLAROUNDPOINT) {
			@Override
			protected void configureChannel(Channel channel, TargetPoint point) {
				super.configureChannel(channel, point);
				setTargetAttr(channel, point);
			}
		};
	}

	public static IPacketSender createSender(Channel channel, OutboundTarget target) {
		return new FmlPacketSender(channel, target);
	}

	private static class FmlPacketSender extends PacketSenderBase {
		private final OutboundTarget target;

		public FmlPacketSender(Channel channel, OutboundTarget target) {
			super(channel);
			this.target = target;
		}

		@Override
		protected void configureChannel(Channel channel) {
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(target);
		}
	}

	private static class FmlTargetedPacketSender<T> extends TargetedPacketSenderBase<T> {

		private final OutboundTarget selector;

		public FmlTargetedPacketSender(Channel channel, OutboundTarget target) {
			super(channel);
			this.selector = target;
		}

		@Override
		protected void configureChannel(Channel channel, T target) {
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(selector);
		}
	}

}
