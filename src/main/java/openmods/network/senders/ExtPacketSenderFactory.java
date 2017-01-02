package openmods.network.senders;

import io.netty.channel.Channel;
import java.util.Collection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import openmods.network.DimCoord;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.IPacketTargetSelector;
import openmods.network.targets.SelectChunkWatchers;
import openmods.network.targets.SelectEntityWatchers;
import openmods.network.targets.SelectMultiplePlayers;

public class ExtPacketSenderFactory {

	public static <T> ITargetedPacketSender<T> createSender(Channel channel, IPacketTargetSelector<T> selector) {
		return new ExtTargetedPacketSender<T>(channel, selector);
	}

	public static ITargetedPacketSender<DimCoord> createBlockSender(Channel channel) {
		return createSender(channel, SelectChunkWatchers.INSTANCE);
	}

	public static ITargetedPacketSender<Entity> createEntitySender(Channel channel) {
		return createSender(channel, SelectEntityWatchers.INSTANCE);
	}

	public static ITargetedPacketSender<Collection<EntityPlayerMP>> createMultiplePlayersSender(Channel channel) {
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
