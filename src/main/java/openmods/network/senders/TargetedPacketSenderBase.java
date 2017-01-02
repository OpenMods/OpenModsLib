package openmods.network.senders;

import io.netty.channel.Channel;
import java.util.Collection;
import openmods.utils.NetUtils;

public class TargetedPacketSenderBase<T> implements ITargetedPacketSender<T> {

	private final Channel channel;

	public TargetedPacketSenderBase(Channel channel) {
		this.channel = channel;
	}

	protected void configureChannel(Channel channel, T target) {}

	protected void cleanupChannel(Channel channel) {}

	@Override
	public void sendMessage(Object msg, T target) {
		configureChannel(channel, target);
		channel.writeAndFlush(msg).addListener(NetUtils.LOGGING_LISTENER);
		cleanupChannel(channel);
	}

	@Override
	public void sendMessages(Collection<Object> msgs, T target) {
		configureChannel(channel, target);

		for (Object msg : msgs)
			channel.write(msg).addListener(NetUtils.LOGGING_LISTENER);

		channel.flush();
		cleanupChannel(channel);
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
