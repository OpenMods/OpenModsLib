package openmods.network.senders;

import io.netty.channel.Channel;
import java.util.Collection;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.IPacketTargetSelector;
import openmods.network.targets.SelectMultiplePlayers;

public class ExtPacketSenderFactory {

	public static <T> ITargetedPacketSender<T> createSender(Channel channel, IPacketTargetSelector<T> selector) {
		return new ExtTargetedPacketSender<>(channel, selector);
	}

	public static ITargetedPacketSender<Collection<ServerPlayerEntity>> createMultiplePlayersSender(Channel channel) {
		return createSender(channel, SelectMultiplePlayers.INSTANCE);
	}

	private static class ExtTargetedPacketSender<T> extends TargetedPacketSenderBase<T> {

		public final IPacketTargetSelector<T> selector;

		public ExtTargetedPacketSender(Channel channel, IPacketTargetSelector<T> selector) {
			super(channel);
			this.selector = selector;
		}

		@Override
		protected void configureChannel(Channel channel, T target) {
			channel.attr(ExtendedOutboundHandler.MESSAGETARGET).set(selector);
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(target);
		}

		@Override
		protected void cleanupChannel(Channel channel) {
			channel.attr(ExtendedOutboundHandler.MESSAGETARGET).set(null);
		}

	}

}
