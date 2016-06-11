package openmods.network.senders;

import cpw.mods.fml.common.network.FMLOutboundHandler;
import io.netty.channel.Channel;
import java.util.Collection;
import openmods.utils.NetUtils;

public class TargetedPacketSenderBase<T> implements ITargetedPacketSender<T> {

	private final Channel channel;

	public TargetedPacketSenderBase(Channel channel) {
		this.channel = channel;
	}

	protected void configureChannel(Channel channel, T target) {}

	protected void setTargetAttr(Channel channel, Object attr) {
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(attr);
	}

	@Override
	public void sendMessage(Object msg, T target) {
		configureChannel(channel, target);
		channel.writeAndFlush(msg).addListener(NetUtils.LOGGING_LISTENER);
	}

	@Override
	public void sendMessages(Collection<Object> msgs, T target) {
		configureChannel(channel, target);

		for (Object msg : msgs)
			channel.write(msg).addListener(NetUtils.LOGGING_LISTENER);

		channel.flush();
	}

	@Override
	public IPacketSender bind(final T target) {
		return new IPacketSender() {
			@Override
			public void sendMessage(Object msg) {
				TargetedPacketSenderBase.this.sendMessage(msg, target);
			}

			@Override
			public void sendMessages(Collection<Object> msg) {
				TargetedPacketSenderBase.this.sendMessages(msg, target);
			}
		};
	}
}
