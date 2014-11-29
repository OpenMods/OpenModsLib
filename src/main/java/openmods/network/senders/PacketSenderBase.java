package openmods.network.senders;

import io.netty.channel.Channel;
import openmods.utils.NetUtils;

public class PacketSenderBase<M> implements IPacketSender<M> {

	private final Channel channel;

	public PacketSenderBase(Channel channel) {
		this.channel = channel;
	}

	protected void configureChannel(Channel channel) {}

	@Override
	public void sendPacket(M msg) {
		configureChannel(channel);
		channel.writeAndFlush(msg).addListener(NetUtils.LOGGING_LISTENER);
	}
}
