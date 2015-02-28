package openmods.network.senders;

import io.netty.channel.Channel;
import net.minecraft.entity.Entity;
import openmods.network.DimCoord;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.IPacketTargetSelector;
import openmods.network.targets.SelectChunkWatchers;
import openmods.network.targets.SelectEntityWatchers;

public class ExtPacketSenderFactory {

	public static <M> ITargetedPacketSender<DimCoord> createBlockSender(Channel channel) {
		return new ExtTargetedPacketSender<DimCoord>(channel, SelectChunkWatchers.INSTANCE) {
			@Override
			protected void configureChannel(Channel channel, DimCoord target) {
				super.configureChannel(channel, target);
				setTargetAttr(channel, target);
			}
		};
	}

	public static <M> ITargetedPacketSender<Entity> createEntitySender(Channel channel) {
		return new ExtTargetedPacketSender<Entity>(channel, SelectEntityWatchers.INSTANCE) {
			@Override
			protected void configureChannel(Channel channel, Entity target) {
				super.configureChannel(channel, target);
				setTargetAttr(channel, target);
			}
		};
	}

	private static class ExtTargetedPacketSender<T> extends TargetedPacketSenderBase<T> {

		public final IPacketTargetSelector selector;

		public ExtTargetedPacketSender(Channel channel, IPacketTargetSelector selector) {
			super(channel);
			this.selector = selector;
		}

		@Override
		protected void configureChannel(Channel channel, T target) {
			channel.attr(ExtendedOutboundHandler.MESSAGETARGET).set(selector);
		}
	}

}
