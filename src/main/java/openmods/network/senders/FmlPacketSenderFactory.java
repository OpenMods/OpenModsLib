package openmods.network.senders;

import io.netty.channel.Channel;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class FmlPacketSenderFactory {

	public static <M> ITargetedPacketSender<M, EntityPlayer> createPlayerSender(Channel channel) {
		return new FmlTargetedPacketSender<M, EntityPlayer>(channel, OutboundTarget.PLAYER) {
			@Override
			protected void configureChannel(Channel channel, EntityPlayer player) {
				super.configureChannel(channel, player);
				setTargetAttr(channel, player);
			}
		};
	}

	public static <M> ITargetedPacketSender<M, Integer> createDimensionSender(Channel channel) {
		return new FmlTargetedPacketSender<M, Integer>(channel, OutboundTarget.DIMENSION) {
			@Override
			protected void configureChannel(Channel channel, Integer dimensionId) {
				super.configureChannel(channel, dimensionId);
				setTargetAttr(channel, dimensionId);
			}
		};
	}

	public static <M> ITargetedPacketSender<M, TargetPoint> createPointSender(Channel channel) {
		return new FmlTargetedPacketSender<M, TargetPoint>(channel, OutboundTarget.ALLAROUNDPOINT) {
			@Override
			protected void configureChannel(Channel channel, TargetPoint point) {
				super.configureChannel(channel, point);
				setTargetAttr(channel, point);
			}
		};
	}

	public static <M> IPacketSender<M> createGlobalSender(Channel channel) {
		return new FmlPacketSender<M>(channel, OutboundTarget.ALL);
	}

	public static <M> IPacketSender<M> createClientSender(Channel channel) {
		return new FmlPacketSender<M>(channel, OutboundTarget.TOSERVER);
	}

	private static class FmlPacketSender<M> extends PacketSenderBase<M> {

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

	private static class FmlTargetedPacketSender<M, T> extends TargetedPacketSenderBase<M, T> {

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
